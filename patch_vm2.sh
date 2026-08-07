sed -i '394,401d' app/src/main/java/com/example/features/home/HomeViewModel.kt
sed -i '392a \    fun saveAppClassification(pkg: String, name: String, classification: String) {\n        viewModelScope.launch {\n            repository.saveAppClassification(pkg, name, classification)\n        }\n    }' app/src/main/java/com/example/features/home/HomeViewModel.kt
