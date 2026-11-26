package co.edu.unal.reto11;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private LinearLayout messages; private EditText prompt; private Button send; private TextView mode; private ScrollView scroll;
    @Override public void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_main);messages=findViewById(R.id.messages);prompt=findViewById(R.id.prompt);send=findViewById(R.id.send);mode=findViewById(R.id.mode_status);scroll=(ScrollView)((View)messages.getParent()); addBubble("Hola, soy Lumi. Puedo ayudarte a convertir una idea en un plan, texto o solución.",false); if(BuildConfig.GEMINI_API_KEY.isEmpty())mode.setText("Modo demo · agrega GEMINI_API_KEY para usar Gemini"); send.setOnClickListener(v->ask());}
    private void ask(){String question=prompt.getText().toString().trim();if(question.isEmpty())return; prompt.setText(""); addBubble(question,true); send.setEnabled(false); mode.setText("Pensando…"); executor.execute(()->{String answer;try{answer=BuildConfig.GEMINI_API_KEY.isEmpty()?demoAnswer(question):callGemini(question);}catch(Exception e){answer="No pude conectar con el modelo ahora. Revisa Internet o la clave de Gemini.";}String finalAnswer=answer;runOnUiThread(()->{addBubble(finalAnswer,false);send.setEnabled(true);mode.setText(BuildConfig.GEMINI_API_KEY.isEmpty()?"Modo demo · respuesta local":"Gemini conectado");});});}
    private String demoAnswer(String q){return "Modo demo: recibí tu prompt sobre ‘"+q+"’. Para activar el modelo generativo real, agrega GEMINI_API_KEY en local.properties y vuelve a compilar. La interfaz ya está preparada para enviar el prompt y mostrar la respuesta.";}
    private String callGemini(String question)throws Exception{JSONObject part=new JSONObject().put("text",question);JSONArray parts=new JSONArray().put(part);JSONObject content=new JSONObject().put("parts",parts);JSONObject body=new JSONObject().put("contents",new JSONArray().put(content));URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="+BuildConfig.GEMINI_API_KEY);HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()))){StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);JSONObject response=new JSONObject(b.toString());return response.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");}finally{c.disconnect();}}
    private void addBubble(String value,boolean user){TextView bubble=new TextView(this);bubble.setText(value);bubble.setTextSize(15);bubble.setTextColor(user?0xffffffff:0xff27164f);bubble.setTypeface(null,user?Typeface.NORMAL:Typeface.NORMAL);bubble.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable bg=new GradientDrawable();bg.setColor(user?0xff7b4dff:0xfff0ecff);bg.setCornerRadius(dp(18));bubble.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(dp(20),dp(5),dp(20),dp(5));p.gravity=user?Gravity.END:Gravity.START;bubble.setLayoutParams(p);messages.addView(bubble);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
