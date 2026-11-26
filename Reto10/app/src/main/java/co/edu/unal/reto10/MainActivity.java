package co.edu.unal.reto10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText search; private Button button; private Button aiButton; private TextView status; private ProgressBar loading; private LinearLayout results;
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_main); bind(); button.setOnClickListener(v -> load(search.getText().toString().trim())); aiButton.setOnClickListener(v -> openAi()); load(""); }
    private void bind() { search=findViewById(R.id.search_input); button=findViewById(R.id.search_button); aiButton=findViewById(R.id.ai_button); status=findViewById(R.id.status); loading=findViewById(R.id.loading); results=findViewById(R.id.results); }
    private void openAi() { Intent launch=getPackageManager().getLaunchIntentForPackage("co.edu.unal.reto11"); if(launch!=null) startActivity(launch); else status.setText("Instala el módulo Reto11 para abrir Lumi AI."); }
    private void load(String term) {
        loading.setVisibility(View.VISIBLE); button.setEnabled(false); status.setText("Consultando el Portal Nacional de Datos Abiertos…");
        executor.execute(() -> { try { String address="https://www.datos.gov.co/resource/jbjy-vk9h.json?$limit=20&$order=fecha_de_firma%20DESC"; if(!term.isEmpty()) address += "&$q="+URLEncoder.encode(term,"UTF-8"); JSONArray rows=new JSONArray(get(address)); runOnUiThread(() -> render(rows)); } catch(Exception e) { runOnUiThread(() -> { loading.setVisibility(View.GONE); button.setEnabled(true); status.setText("No se pudo consultar el servicio. Revisa tu conexión."); }); } });
    }
    private String get(String address) throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestProperty("Accept","application/json"); try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()))) { StringBuilder b=new StringBuilder(); String line; while((line=r.readLine())!=null)b.append(line); return b.toString(); } finally { c.disconnect(); } }
    private void render(JSONArray rows) { results.removeAllViews(); loading.setVisibility(View.GONE); button.setEnabled(true); status.setText(String.format(Locale.getDefault(), "%d resultados · Fuente: SECOP II", rows.length())); for(int i=0;i<rows.length();i++){ JSONObject row=rows.optJSONObject(i); if(row!=null) results.addView(card(row)); } }
    private View card(JSONObject row) { LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16),dp(14),dp(16),dp(14)); GradientDrawable bg=new GradientDrawable(); bg.setColor(0xffffffff); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1),0xffd9e7e7); card.setBackground(bg); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,0,0,dp(10)); card.setLayoutParams(cp); card.addView(text(value(row,"nombre_entidad","Entidad sin nombre"),16,true,0xff102a2e)); TextView meta=text(value(row,"ciudad","Colombia")+" · "+value(row,"estado_contrato","Estado no informado"),13,false,0xff087e83); meta.setPadding(0,dp(6),0,dp(4)); card.addView(meta); TextView desc=text(value(row,"descripcion_del_proceso","Sin descripción disponible"),13,false,0xff597277); desc.setMaxLines(3); card.addView(desc); TextView hint=text("Toca para ver detalles  ›",12,false,0xff7b9294); hint.setPadding(0,dp(10),0,0); card.addView(hint); card.setOnClickListener(v -> showDetails(row)); return card; }
    private void showDetails(JSONObject row) { LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(4),dp(20),dp(4)); addDetail(content,"Entidad",value(row,"nombre_entidad","Sin información")); addDetail(content,"NIT",value(row,"nit_entidad","Sin información")); addDetail(content,"Ubicación",value(row,"ciudad","Sin ciudad")+" · "+value(row,"departamento","Sin departamento")); addDetail(content,"Estado",value(row,"estado_contrato","Sin información")); addDetail(content,"Tipo de contrato",value(row,"tipo_de_contrato","Sin información")); addDetail(content,"Modalidad",value(row,"modalidad_de_contratacion","Sin información")); addDetail(content,"Proceso",value(row,"proceso_de_compra","Sin información")); addDetail(content,"Fecha de firma",value(row,"fecha_de_firma","Sin información")); addDetail(content,"Descripción",value(row,"descripcion_del_proceso","Sin descripción disponible")); new AlertDialog.Builder(this).setTitle("Detalle del contrato").setView(content).setPositiveButton("Cerrar",null).show(); }
    private void addDetail(LinearLayout content,String label,String value){TextView item=text(label+"\n"+value,14,false,0xff36575b); item.setPadding(0,dp(8),0,dp(8)); content.addView(item);}
    private TextView text(String value,int size,boolean bold,int color){ TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,Typeface.BOLD); return t; }
    private String value(JSONObject o,String key,String fallback){String v=o.optString(key,"").trim();return v.isEmpty()?fallback:v;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
