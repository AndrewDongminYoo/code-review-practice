import 'package:flutter/foundation.dart';

import 'studio_repository.dart';

class SearchController extends ChangeNotifier {
  SearchController(this._repository);

  final StudioRepository _repository;
  String _query = '';
  List<Studio> _studios = const [];
  bool _loading = false;
  String? _error;

  String get query => _query;
  List<Studio> get studios => List.unmodifiable(_studios);
  bool get loading => _loading;
  String? get error => _error;

  Future<void> search(String input) async {
    final normalized = input.trim();
    _query = normalized;
    _loading = true;
    _studios = const [];
    notifyListeners();

    try {
      final found = await _repository.search(normalized);
      _studios = found;
    } on SearchUnavailable {
      _error = 'Could not load studios. Please try again.';
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> retry() => search(_query);

  void clear() {
    _query = '';
    _studios = const [];
    _loading = false;
    _error = null;
    notifyListeners();
  }
}
