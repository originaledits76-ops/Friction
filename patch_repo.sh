sed -i 's/val allTasks: Flow<List<FrictionTask>> = dao.getAllTasks()/val allTasks: Flow<List<FrictionTask>> = dao.getAllTasks()\n    val allAppClassifications: Flow<List<com.example.data.model.AppClassification>> = dao.getAllAppClassifications()/' app/src/main/java/com/example/data/repository/FrictionRepository.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/data/repository/FrictionRepository.kt

    suspend fun saveAppClassification(pkg: String, name: String, classification: String) {
        val entry = com.example.data.model.AppClassification(pkg, name, classification)
        dao.insertAppClassification(entry)
        val uid = currentUserUid
        if (uid != null) {
            firestoreService.db.collection("users").document(uid)
                .collection("appClassifications").document(pkg.replace("/", "_"))
                .set(mapOf(
                    "packageName" to pkg,
                    "appName" to name,
                    "classification" to classification,
                    "updatedAt" to System.currentTimeMillis()
                ), SetOptions.merge())
        }
    }
INNER_EOF
