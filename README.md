# Reto 0

Aplicacion Android creada para la tarea **Reto 0**.

La aplicacion usa Kotlin y Jetpack Compose. La pantalla principal muestra unicamente el texto:

```text
Hola Mundo
```

El texto esta centrado horizontal y verticalmente.

## Datos del proyecto

- Nombre de la aplicacion: Reto 0
- Package: `com.example.reto0`
- Lenguaje: Kotlin
- Interfaz: Jetpack Compose
- Repositorio sugerido en GitHub: `reto-0`

## Abrir en Android Studio

1. Abre Android Studio.
2. Selecciona **Open**.
3. Elige esta carpeta:

   ```text
   C:\Users\Stepe\Pictures\Nueva carpeta\DADM-2026-2
   ```

4. Espera a que Android Studio sincronice Gradle.
5. Selecciona un emulador o conecta un celular Android con depuracion USB.
6. Presiona **Run** para ejecutar la aplicacion.

## Compilar desde terminal

En PowerShell, dentro de la carpeta del proyecto, ejecuta:

```powershell
.\gradlew.bat assembleDebug
```

Si la compilacion termina correctamente, el APK de depuracion queda en:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Subir a GitHub

Si tu repositorio remoto ya existe y esta conectado, usa:

```powershell
git add .
git commit -m "Crear proyecto Android Reto 0"
git push -u origin main
```

Si necesitas crear o conectar el repositorio manualmente, crea un repositorio privado llamado `reto-0` en GitHub y luego ejecuta:

```powershell
git remote add origin https://github.com/TU_USUARIO/reto-0.git
git add .
git commit -m "Crear proyecto Android Reto 0"
git branch -M main
git push -u origin main
```
