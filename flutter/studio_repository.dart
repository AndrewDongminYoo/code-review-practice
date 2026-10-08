class Studio {
  const Studio({required this.id, required this.name});

  final String id;
  final String name;
}

class SearchUnavailable implements Exception {
  const SearchUnavailable();
}

abstract class StudioRepository {
  // Accepts trimmed text, including empty text (returns all studios).
  // Completes with an immutable result or SearchUnavailable.
  // Independent requests may finish in any order. No cancellation API.
  Future<List<Studio>> search(String query);
}
