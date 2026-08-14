sed -i -e '/fun verifyPremiumEntitlement/,/    }/c \
    fun verifyPremiumEntitlement(user: com.example.data.model.User, onResult: (Boolean) -> Unit) {\
        viewModelScope.launch {\
            val userRepository = com.example.data.repository.UserRepository(com.example.data.repository.FirestoreService())\
            val fetchedUser = userRepository.getUser(user.uid)\
            if (fetchedUser != null) {\
                val isEntitled = fetchedUser.premium || (fetchedUser.isTrialActive && !fetchedUser.hasTrialExpired())\
                onResult(isEntitled)\
            } else {\
                onResult(false)\
            }\
        }\
    }' app/src/main/java/com/example/features/home/HomeViewModel.kt
