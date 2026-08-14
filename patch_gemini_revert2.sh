sed -i 's/val groqApiKey = try { "" }/val groqApiKey = try { BuildConfig.GROQ_API_KEY }/' app/src/main/java/com/example/data/service/GeminiService.kt
