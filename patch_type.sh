sed -i 's/FontFamily.Default/SfProDisplay/g' app/src/main/java/com/example/ui/theme/Type.kt
sed -i '10a val SfProDisplay = FontFamily(\n    Font(R.font.sf_pro_display, FontWeight.Normal)\n)' app/src/main/java/com/example/ui/theme/Type.kt
