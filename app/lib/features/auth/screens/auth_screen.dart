import 'package:confetti/confetti.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:google_sign_in/google_sign_in.dart';
import '../../../core/config/app_config.dart';
import '../../../core/widgets/deco_circle.dart';
import '../providers/auth_provider.dart';
import '../widgets/auth_form.dart';
import '../widgets/desktop_auth.dart';
import '../widgets/tab_switcher.dart';

class AuthScreen extends ConsumerStatefulWidget {
  const AuthScreen({super.key});

  @override
  ConsumerState<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends ConsumerState<AuthScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confettiController = ConfettiController(
    duration: const Duration(milliseconds: 1200),
  );
  final _googleSignIn = GoogleSignIn(
    scopes: AppConfig.googleScopes,
    serverClientId: AppConfig.googleServerClientId,
  );

  AuthMode _mode = AuthMode.signIn;
  bool _obscurePassword = true;
  bool _isLoading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confettiController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isLoading = true);
    try {
      final notifier = ref.read(authProvider.notifier);
      if (_mode == AuthMode.signIn) {
        await notifier.login(
          _emailController.text.trim(),
          _passwordController.text,
        );
      } else {
        await notifier.register(
          _emailController.text.trim(),
          _passwordController.text,
          _nameController.text.trim(),
        );
      }
      if (mounted) _confettiController.play();
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        _showError(e);
      }
    }
  }

  Future<void> _googleSignInTap() async {
    setState(() => _isLoading = true);
    try {
      final account = await _googleSignIn.signIn();
      if (account == null) {
        if (mounted) setState(() => _isLoading = false);
        return;
      }
      final auth = await account.authentication;
      final idToken = auth.idToken;
      if (idToken == null) {
        if (mounted) {
          setState(() => _isLoading = false);
          _showSnack('Google sign-in failed. Please try again.');
        }
        return;
      }
      await ref.read(authProvider.notifier).googleSignIn(idToken);
      if (mounted) _confettiController.play();
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        _showError(e);
      }
    }
  }

  void _showError(Object? error) {
    String message = 'Something went wrong. Please try again.';
    if (error is DioException) {
      final status = error.response?.statusCode;
      final data = error.response?.data;
      final serverMessage = data is Map ? data['message'] as String? : null;
      if (serverMessage != null) {
        message = serverMessage;
      } else if (status == 401 || status == 403) {
        message = 'Incorrect email or password.';
      } else if (status == 409) {
        message = 'An account with this email already exists.';
      }
    }
    _showSnack(message);
  }

  void _showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: GoogleFonts.nunito(fontWeight: FontWeight.w600),
        ),
        backgroundColor: const Color(0xFF1A1A2E),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDesktop = MediaQuery.of(context).size.width >= 600;

    final form = AuthForm(
      formKey: _formKey,
      nameController: _nameController,
      emailController: _emailController,
      passwordController: _passwordController,
      confettiController: _confettiController,
      mode: _mode,
      obscurePassword: _obscurePassword,
      isLoading: _isLoading,
      onModeChanged: (m) => setState(() => _mode = m),
      onTogglePassword: () =>
          setState(() => _obscurePassword = !_obscurePassword),
      onSubmit: _isLoading ? null : _submit,
      onGoogleSignIn: _isLoading ? null : _googleSignInTap,
    );

    if (isDesktop) {
      return DesktopAuthShell(form: form);
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF4F8FF),
      body: Stack(
        children: [
          const Positioned(
            top: -60,
            right: -55,
            child: DecoCircle(color: Color(0xFFA0C4FF), size: 190),
          ),
          const Positioned(
            bottom: 80,
            left: -35,
            child: DecoCircle(color: Color(0xFFC77DFF), size: 120),
          ),
          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(28, 24, 28, 40),
              child: form,
            ),
          ),
          Align(
            alignment: Alignment.topCenter,
            child: ConfettiWidget(
              confettiController: _confettiController,
              blastDirectionality: BlastDirectionality.explosive,
              numberOfParticles: 22,
              colors: const [
                Color(0xFF7BB3FF),
                Color(0xFF9B8FFF),
                Color(0xFF5BBFB8),
                Color(0xFFC77DFF),
                Color(0xFFBDB2FF),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
