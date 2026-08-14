sed -i -e '/if (rules.size >= 2) {/,/showWizard = true/c \
                            if (rules.size >= 2) {\
                                if (user != null && homeViewModel != null) {\
                                    homeViewModel.verifyPremiumEntitlement(user) { isEntitled ->\
                                        if (isEntitled) {\
                                            showWizard = true\
                                        } else {\
                                            showLockedSheet = true\
                                        }\
                                    }\
                                }\
                            } else {\
                                showWizard = true\
                            }' app/src/main/java/com/example/features/settings/SettingsScreen.kt
