sed -i -e '/fun markEarlyBirdOfferSeen/i \
    fun verifyPremiumEntitlement(onResult: (Boolean) -> Unit) {\
        viewModelScope.launch {\
            val fetchedUser = userRepository.getUser(_user.value.uid)\
            if (fetchedUser != null) {\
                _user.value = fetchedUser\
                val isEntitled = fetchedUser.premium || (fetchedUser.isTrialActive && !fetchedUser.hasTrialExpired())\
                onResult(isEntitled)\
            } else {\
                onResult(false)\
            }\
        }\
    }' app/src/main/java/com/example/features/home/HomeViewModel.kt
