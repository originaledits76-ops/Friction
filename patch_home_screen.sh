sed -i -e '/onOpenPaywall = { showPaywall = true }/a \
                                onVerifyEntitlement = { cb -> homeViewModel.verifyPremiumEntitlement(cb) }' app/src/main/java/com/example/features/home/HomeScreen.kt
