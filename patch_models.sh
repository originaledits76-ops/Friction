cat << 'INNER_EOF' >> app/src/main/java/com/example/data/model/FrictionModels.kt

@Keep
@Entity(tableName = "app_classifications")
data class AppClassification(
    @PrimaryKey val packageName: String = "",
    val appName: String = "",
    val classification: String = "DISTRACTING" // "DISTRACTING" or "PRODUCTIVE"
)
INNER_EOF
