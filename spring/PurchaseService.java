package practice;

import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {
    private final ListingRepository listings;
    private final PurchaseRepository purchases;
    private final PaymentStub payments;

    @PersistenceContext
    private EntityManager entityManager;

    public PurchaseService(ListingRepository listings, PurchaseRepository purchases,
                           PaymentStub payments) {
        this.listings = listings;
        this.purchases = purchases;
        this.payments = payments;
    }

    @Transactional(rollbackFor = PaymentDeclined.class)
    public PurchaseView buy(long callerId, BuyRequest request) throws PaymentDeclined {
        if (callerId != request.buyerId()) {
            throw new Forbidden();
        }

        Listing listing = entityManager.find(
                Listing.class, request.listingId(), LockModeType.PESSIMISTIC_WRITE);
        if (listing == null) {
            throw new NotFound();
        }
        if (listing.getRemaining() < request.quantity()) {
            throw new OutOfStock();
        }
        BigDecimal total = listing.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.quantity()));
        listing.setRemaining(listing.getRemaining() - request.quantity());
        listings.saveAndFlush(listing);
        payments.authorize(callerId, total);
        Purchase purchase = purchases.save(new Purchase(
                callerId, listing.getId(), request.quantity(), total));
        return new PurchaseView(purchase.getId(), purchase.getBuyerId(), total);
    }
}
