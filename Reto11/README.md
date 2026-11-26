# Reto 11 · Lumi AI

Chatbot Android con integración REST a Gemini. La clave no se guarda en Git.

## Configuración completa

1. Crea una clave en [Google AI Studio](https://aistudio.google.com/apikey) o en Google Cloud.
2. En el archivo `local.properties` de la raíz del proyecto agrega:

   `GEMINI_API_KEY=pega_aqui_tu_clave`

3. Sincroniza Gradle y ejecuta `Reto11` o instala también Reto10: su botón inferior **Hablar con Lumi AI** abre esta app.
4. Si no agregas la clave, el chatbot funciona en modo demo local y muestra claramente el estado.

No subas `local.properties` ni la clave a GitHub. Para cancelarla, abre Google Cloud Console → **APIs y servicios** → **Credenciales** → **Claves de API**, selecciona la clave y pulsa **Eliminar**. También puedes regenerarla si solo quieres reemplazarla. La eliminación de la clave detiene su uso; no cancela por sí sola una cuenta de facturación.
