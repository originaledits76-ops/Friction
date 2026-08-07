sed -i 's/val challenges: StateFlow<List<Challenge>> = repository.allChallenges\n        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())/val challenges: StateFlow<List<Challenge>> = repository.allChallenges\n        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())\n\n    val appClassifications: StateFlow<List<com.example.data.model.AppClassification>> = repository.allAppClassifications\n        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())/g' app/src/main/java/com/example/features/home/HomeViewModel.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/features/home/HomeViewModel.kt

    fun saveAppClassification(pkg: String, name: String, classification: String) {
        viewModelScope.launch {
            repository.saveAppClassification(pkg, name, classification)
        }
    }
INNER_EOF
