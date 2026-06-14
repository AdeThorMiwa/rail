sealed class ConnieSpan {
  const ConnieSpan();

  factory ConnieSpan.fromJson(Map<String, dynamic> json) {
    return switch (json['type'] as String) {
      'text' => ConnieTextSpan.fromJson(json),
      'cta' => CtaSpan.fromJson(json),
      _ => throw ArgumentError('Unknown span type: ${json['type']}'),
    };
  }
}

class ConnieTextSpan extends ConnieSpan {
  final String text;
  const ConnieTextSpan({required this.text});
  factory ConnieTextSpan.fromJson(Map<String, dynamic> json) =>
      ConnieTextSpan(text: json['text'] as String);
}

class CtaSpan extends ConnieSpan {
  final String label;
  final String command;
  final Map<String, dynamic> params;
  const CtaSpan({required this.label, required this.command, this.params = const {}});
  factory CtaSpan.fromJson(Map<String, dynamic> json) => CtaSpan(
    label: json['label'] as String,
    command: json['command'] as String,
    params: (json['params'] as Map<String, dynamic>?) ?? {},
  );
}

class TextContent {
  final List<ConnieSpan> spans;
  const TextContent({required this.spans});
  factory TextContent.fromJson(Map<String, dynamic> json) => TextContent(
    spans: (json['spans'] as List<dynamic>)
        .map((e) => ConnieSpan.fromJson(e as Map<String, dynamic>))
        .toList(),
  );
}

class ActionItem {
  final String id;
  final String label;
  final String style;
  final String command;
  final Map<String, dynamic> params;
  final bool isAsync;

  const ActionItem({
    required this.id,
    required this.label,
    required this.style,
    required this.command,
    this.params = const {},
    this.isAsync = false,
  });

  factory ActionItem.fromJson(Map<String, dynamic> json) => ActionItem(
    id: json['id'] as String,
    label: json['label'] as String,
    style: json['style'] as String? ?? 'primary',
    command: json['command'] as String,
    params: (json['params'] as Map<String, dynamic>?) ?? {},
    isAsync: (json['mode'] as String?) == 'async',
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'label': label,
    'style': style,
    'command': command,
    'params': params,
    'mode': isAsync ? 'async' : 'sync',
  };
}

sealed class Block {
  final String id;
  const Block({required this.id});

  factory Block.fromJson(Map<String, dynamic> json) {
    return switch (json['type'] as String) {
      'text' => TextBlock.fromJson(json),
      'table' => TableBlock.fromJson(json),
      'list' => ListBlock.fromJson(json),
      'actions' => ActionsBlock.fromJson(json),
      'image' => ImageBlock.fromJson(json),
      'wrap_card' => WrapCardBlock.fromJson(json),
      'redirect' => RedirectBlock.fromJson(json),
      _ => throw ArgumentError('Unknown block type: ${json['type']}'),
    };
  }
}

class TextBlock extends Block {
  final TextContent content;
  const TextBlock({required super.id, required this.content});
  factory TextBlock.fromJson(Map<String, dynamic> json) => TextBlock(
    id: json['id'] as String? ?? '',
    content: TextContent.fromJson(json),
  );
}

class TableBlock extends Block {
  final List<String>? columns;
  final List<List<String>> rows;
  const TableBlock({required super.id, this.columns, required this.rows});
  factory TableBlock.fromJson(Map<String, dynamic> json) => TableBlock(
    id: json['id'] as String? ?? '',
    columns: (json['columns'] as List<dynamic>?)?.map((e) => e as String).toList(),
    rows: (json['rows'] as List<dynamic>)
        .map((r) => (r as List<dynamic>).map((c) => c as String).toList())
        .toList(),
  );
}

class ListBlock extends Block {
  final List<String> items;
  const ListBlock({required super.id, required this.items});
  factory ListBlock.fromJson(Map<String, dynamic> json) => ListBlock(
    id: json['id'] as String? ?? '',
    items: (json['items'] as List<dynamic>)
        .map((e) {
          final item = e as Map<String, dynamic>;
          return item['text'] as String? ?? item['label'] as String? ?? '';
        })
        .toList(),
  );
}

enum EmptyOutcome { stay, disappear }

class ActionsBlock extends Block {
  final List<ActionItem> items;
  final List<ActionItem>? successItems;
  final List<ActionItem>? failureItems;
  final EmptyOutcome onSuccessEmpty;
  final EmptyOutcome onFailureEmpty;

  const ActionsBlock({
    required super.id,
    required this.items,
    this.successItems,
    this.failureItems,
    this.onSuccessEmpty = EmptyOutcome.stay,
    this.onFailureEmpty = EmptyOutcome.stay,
  });

  factory ActionsBlock.fromJson(Map<String, dynamic> json) {
    List<ActionItem> parseItems(dynamic raw) => raw == null
        ? []
        : (raw as List<dynamic>)
            .map((e) => ActionItem.fromJson(e as Map<String, dynamic>))
            .toList();

    EmptyOutcome parseOutcome(String? value, EmptyOutcome fallback) =>
        value == 'disappear' ? EmptyOutcome.disappear : fallback;

    return ActionsBlock(
      id: json['id'] as String? ?? '',
      items: parseItems(json['items']),
      successItems: json['successItems'] != null
          ? parseItems(json['successItems'])
          : null,
      failureItems: json['failureItems'] != null
          ? parseItems(json['failureItems'])
          : null,
      onSuccessEmpty:
          parseOutcome(json['onSuccessEmpty'] as String?, EmptyOutcome.stay),
      onFailureEmpty:
          parseOutcome(json['onFailureEmpty'] as String?, EmptyOutcome.stay),
    );
  }
}

class ImageBlock extends Block {
  final String url;
  final double aspectRatio;
  const ImageBlock({required super.id, required this.url, required this.aspectRatio});
  factory ImageBlock.fromJson(Map<String, dynamic> json) => ImageBlock(
    id: json['id'] as String? ?? '',
    url: json['url'] as String,
    aspectRatio: (json['aspectRatio'] as num).toDouble(),
  );
}

class WrapCardFocusGoal {
  final String title;
  final double completionRate;
  final int completed;
  final int total;

  const WrapCardFocusGoal({
    required this.title,
    required this.completionRate,
    required this.completed,
    required this.total,
  });

  factory WrapCardFocusGoal.fromJson(Map<String, dynamic> j) => WrapCardFocusGoal(
    title: j['title'] as String,
    completionRate: (j['completionRate'] as num).toDouble(),
    completed: j['completed'] as int? ?? 0,
    total: j['total'] as int? ?? 0,
  );
}

class WrapCardHabitHighlight {
  final String title;
  final double adherenceRate;

  const WrapCardHabitHighlight({required this.title, required this.adherenceRate});

  factory WrapCardHabitHighlight.fromJson(Map<String, dynamic> j) => WrapCardHabitHighlight(
    title: j['title'] as String,
    adherenceRate: (j['adherenceRate'] as num).toDouble(),
  );
}

class RedirectBlock extends Block {
  final String route;
  final String label;

  const RedirectBlock({required super.id, required this.route, required this.label});

  factory RedirectBlock.fromJson(Map<String, dynamic> json) => RedirectBlock(
    id: json['id'] as String? ?? '',
    route: json['route'] as String,
    label: json['label'] as String? ?? 'Go',
  );
}

class WrapCardBlock extends Block {
  final String cycleTitle;
  final String period;
  final List<WrapCardFocusGoal> focusGoals;
  final List<WrapCardHabitHighlight> habitHighlights;
  final List<String> keyWins;
  final String summary;

  const WrapCardBlock({
    required super.id,
    required this.cycleTitle,
    required this.period,
    required this.focusGoals,
    required this.habitHighlights,
    required this.keyWins,
    required this.summary,
  });

  factory WrapCardBlock.fromJson(Map<String, dynamic> json) => WrapCardBlock(
    id: json['id'] as String? ?? '',
    cycleTitle: json['cycleTitle'] as String? ?? '',
    period: json['period'] as String? ?? '',
    focusGoals: (json['focusGoals'] as List<dynamic>? ?? [])
        .map((e) => WrapCardFocusGoal.fromJson(e as Map<String, dynamic>))
        .toList(),
    habitHighlights: (json['habitHighlights'] as List<dynamic>? ?? [])
        .map((e) => WrapCardHabitHighlight.fromJson(e as Map<String, dynamic>))
        .toList(),
    keyWins: (json['keyWins'] as List<dynamic>? ?? [])
        .map((e) => e as String)
        .toList(),
    summary: json['summary'] as String? ?? '',
  );
}
