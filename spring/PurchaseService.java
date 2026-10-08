package practice;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {
    private final ListingRepository listings;
    private final PurchaseRepository purchases;
    private final PaymentStub payments;

    public PurchaseService(ListingRepository listings, PurchaseRepository purchases,
                           PaymentStub payments) {
        this.listings = listings;
        this.purchases = purchases;
        this.payments = payments;
    }

    @Transactional
    public PurchaseView buy(long callerId, BuyRequest request) throws PaymentDeclined {
        Listing listing = listings.findById(request.listingId())
                .orElseThrow(NotFound::new);
        if (listing.getRemaining() < request.quantity()) {
            throw new OutOfStock();
        }
        BigDecimal total = request.unitPrice()
                .multiply(BigDecimal.valueOf(request.quantity()));
        listing.setRemaining(listing.getRemaining() - request.quantity());
        listings.saveAndFlush(listing);
        payments.authorize(request.buyerId(), total);
        Purchase purchase = purchases.save(new Purchase(
                request.buyerId(), listing.getId(), request.quantity(), total));
        return new PurchaseView(purchase.getId(), purchase.getBuyerId(), total);
    }
}
