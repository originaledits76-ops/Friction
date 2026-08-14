sed -i -e '/val scope = rememberCoroutineScope()/a \
    var isPremiumVerified by remember(user?.premium, user?.isTrialActive) { mutableStateOf(user?.premium == true || (user?.isTrialActive == true && !user.hasTrialExpired())) }\
    LaunchedEffect(Unit) {\
        onVerifyEntitlement { isPremiumVerified = it }\
    }' app/src/main/java/com/example/features/analytics/AnalyticsScreen.kt
sed -i 's/user?.premium != true/!isPremiumVerified/g' app/src/main/java/com/example/features/analytics/AnalyticsScreen.kt
