sed -i -e '/if (rules.size >= 2 && user?.premium != true) {/,/}/c \
                            if (rules.size >= 2) {\
                                homeViewModel.verifyPremiumEntitlement { isEntitled ->\
                                    if (isEntitled) {\
                                        showWizard = true\
                                    } else {\
                                        showLockedSheet = true\
                                    }\
                                }\
                            } else {\
                                showWizard = true\
                            }' app/src/main/java/com/example/features/settings/SettingsScreen.kt
