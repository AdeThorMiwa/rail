import 'package:confetti/confetti.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'auth_gradient_button.dart';
import 'google_button.dart';
import 'rail_field.dart';
import 'tab_switcher.dart';

class AuthForm extends StatelessWidget {
  final GlobalKey<FormState> formKey;
  final TextEditingController nameController;
  final TextEditingController emailController;
  final TextEditingController passwordController;
  final ConfettiController confettiController;
  final AuthMode mode;
  final bool obscurePassword;
  final bool isLoading;
  final ValueChanged<AuthMode> onModeChanged;
  final VoidCallback onTogglePassword;
  final VoidCallback? onSubmit;
  final VoidCallback? onGoogleSignIn;

  const AuthForm({
    super.key,
    required this.formKey,
    required this.nameController,
    required this.emailController,
    required this.passwordController,
    required this.confettiController,
    required this.mode,
    required this.obscurePassword,
    required this.isLoading,
    required this.onModeChanged,
    required this.onTogglePassword,
    required this.onSubmit,
    required this.onGoogleSignIn,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text.rich(
          TextSpan(children: [
            const TextSpan(text: '🚂 '),
            TextSpan(
              text: 'Rail',
              style: GoogleFonts.nunito(
                fontSize: 38,
                fontWeight: FontWeight.w900,
                color: const Color(0xFF7B6EFF),
              ),
            ),
          ]),
        ),
        const SizedBox(height: 6),
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 250),
          child: Text(
            mode == AuthMode.signIn ? 'Welcome back!' : "Let's get started! 🎉",
            key: ValueKey(mode),
            style: GoogleFonts.nunito(
              fontSize: 22,
              fontWeight: FontWeight.w800,
              color: const Color(0xFF1A1A2E),
            ),
          ),
        ),
        const SizedBox(height: 4),
        Text(
          mode == AuthMode.signIn
              ? 'Sign in to keep the momentum going'
              : 'Create your Rail account',
          style: GoogleFonts.nunito(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: const Color(0xFF9090AA),
          ),
        ),
        const SizedBox(height: 28),
        TabSwitcher(mode: mode, onChanged: onModeChanged),
        const SizedBox(height: 20),
        Form(
          key: formKey,
          child: Column(
            children: [
              AnimatedSize(
                duration: const Duration(milliseconds: 380),
                curve: Curves.easeInOut,
                child: mode == AuthMode.register
                    ? Padding(
                        padding: const EdgeInsets.only(bottom: 14),
                        child: RailField(
                          controller: nameController,
                          label: 'Your name',
                          hint: 'What should Rail call you?',
                          focusBorderColor: const Color(0xFF7BB3FF),
                          focusGlow: const Color(0xFF7BB3FF),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Required'
                              : null,
                        ),
                      )
                    : const SizedBox.shrink(),
              ),
              RailField(
                controller: emailController,
                label: 'Email',
                hint: 'you@example.com',
                keyboardType: TextInputType.emailAddress,
                focusBorderColor: const Color(0xFF7BB3FF),
                focusGlow: const Color(0xFF7BB3FF),
                validator: (v) {
                  if (v == null || v.trim().isEmpty) return 'Required';
                  if (!v.contains('@')) return 'Enter a valid email';
                  return null;
                },
              ),
              const SizedBox(height: 14),
              RailField(
                controller: passwordController,
                label: 'Password',
                hint: '••••••••',
                obscureText: obscurePassword,
                focusBorderColor: const Color(0xFF9B8FFF),
                focusGlow: const Color(0xFF9B8FFF),
                suffix: IconButton(
                  icon: Icon(
                    obscurePassword
                        ? Icons.visibility_off_outlined
                        : Icons.visibility_outlined,
                    color: const Color(0xFF9090AA),
                    size: 20,
                  ),
                  onPressed: onTogglePassword,
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Required';
                  if (mode == AuthMode.register && v.length < 8) {
                    return 'Minimum 8 characters';
                  }
                  return null;
                },
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        AuthGradientButton(
          label: mode == AuthMode.signIn ? 'Sign in ✨' : 'Create account ✨',
          colors: const [Color(0xFF9B8FFF), Color(0xFF7B6EFF)],
          shadowColor: const Color(0xFF7B6EFF),
          isLoading: isLoading,
          onTap: onSubmit,
        ),
        const SizedBox(height: 22),
        Row(
          children: [
            Expanded(
              child: Divider(color: Colors.black.withValues(alpha: 0.07), thickness: 2),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14),
              child: Text(
                'or continue with',
                style: GoogleFonts.nunito(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: const Color(0xFFC8C8DC),
                ),
              ),
            ),
            Expanded(
              child: Divider(color: Colors.black.withValues(alpha: 0.07), thickness: 2),
            ),
          ],
        ),
        const SizedBox(height: 22),
        GoogleButton(onTap: onGoogleSignIn),
      ],
    );
  }
}
