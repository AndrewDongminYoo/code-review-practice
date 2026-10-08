import 'package:flutter/foundation.dart';

import 'studio_repository.dart';

class SearchController extends ChangeNotifier {
  SearchController(this._repository);

  final StudioRepository _repository;
  String _query = '';
  List<Studio> _studios = const [];
  bool _loading = false;
  String? _error;
  int _requestGeneration = 0;
  bool _disposed = false;

  String get query => _query;
  List<Studio> get studios => List.unmodifiable(_studios);
  bool get loading => _loading;
  String? get error => _error;

  Future<void> search(String input) async {
    if (_disposed) return;

    final generation = ++_requestGeneration;
    final normalized = input.trim();
    _query = normalized;
    _loading = true;
    _studios = const [];
    _error = null;
    notifyListeners();

    try {
      final found = await _repository.search(normalized);
      if (!_isCurrent(generation)) return;
      _studios = found;
    } on SearchUnavailable {
      if (!_isCurrent(generation)) return;
      _error = 'Could not load studios. Please try again.';
    } finally {
      if (_isCurrent(generation)) {
        _loading = false;
        notifyListeners();
      }
    }
  }

  Future<void> retry() => search(_query);

  void clear() {
    if (_disposed) return;

    _requestGeneration++;
    _query = '';
    _studios = const [];
    _loading = false;
    _error = null;
    notifyListeners();
  }

  bool _isCurrent(int generation) =>
      !_disposed && generation == _requestGeneration;

  @override
  void dispose() {
    _disposed = true;
    _requestGeneration++;
    super.dispose();
  }
}
