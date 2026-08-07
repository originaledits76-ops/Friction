sed -i 's/TextFieldDefaults.outlinedTextFieldColors(/TextFieldDefaults.colors(/g' app/src/main/java/com/example/features/onboarding/OnboardingScreen.kt
sed -i 's/unfocusedBorderColor = Color(0x30FFFFFF),/unfocusedIndicatorColor = Color(0x30FFFFFF),/g' app/src/main/java/com/example/features/onboarding/OnboardingScreen.kt
sed -i 's/focusedBorderColor = FrictionPrimary,/focusedIndicatorColor = FrictionPrimary,/g' app/src/main/java/com/example/features/onboarding/OnboardingScreen.kt
sed -i 's/modifier = Modifier.background(CardBackground)/modifier = Modifier.background(DarkSurface)/g' app/src/main/java/com/example/features/onboarding/OnboardingScreen.kt
