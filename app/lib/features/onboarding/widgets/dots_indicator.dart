import 'package:flutter/material.dart';

class DotsIndicator extends StatelessWidget {
  final int count;
  final int current;
  final Color activeColor;

  const DotsIndicator({
    super.key,
    required this.count,
    required this.current,
    required this.activeColor,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(count, (i) {
        return AnimatedContainer(
          duration: const Duration(milliseconds: 400),
          curve: Curves.elasticOut,
          margin: const EdgeInsets.symmetric(horizontal: 4),
          width: i == current ? 28 : 8,
          height: 8,
          decoration: BoxDecoration(
            color: i == current ? activeColor : const Color(0xFFDDD8F0),
            borderRadius: BorderRadius.circular(4),
          ),
        );
      }),
    );
  }
}
