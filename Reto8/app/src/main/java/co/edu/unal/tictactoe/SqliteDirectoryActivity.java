package co.edu.unal.tictactoe;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SqliteDirectoryActivity extends Activity {
    private static final String[] CLASSIFICATIONS = {
            "Consultoría", "Desarrollo a la medida", "Fábrica de software",
            "Consultoría y desarrollo", "Consultoría y fábrica",
            "Desarrollo y fábrica", "Consultoría, desarrollo y fábrica"
    };

    private CompanyDbHelper database;
    private EditText searchName;
    private Spinner searchClassification;
    private EditText name;
    private EditText url;
    private EditText phone;
    private EditText email;
    private EditText products;
    private Spinner classification;
    private TextView formTitle;
    private LinearLayout companyList;
    private Company editingCompany;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        database = new CompanyDbHelper(this);
        bindViews();
        configureSpinners();
        configureActions();
        refreshList();
    }

    private void bindViews() {
        searchName = findViewById(R.id.search_name);
        searchClassification = findViewById(R.id.search_classification);
        name = findViewById(R.id.company_name);
        url = findViewById(R.id.company_url);
        phone = findViewById(R.id.company_phone);
        email = findViewById(R.id.company_email);
        products = findViewById(R.id.company_products);
        classification = findViewById(R.id.company_classification);
        formTitle = findViewById(R.id.form_title);
        companyList = findViewById(R.id.company_list);
    }

    private void configureSpinners() {
        List<String> filters = new ArrayList<>();
        filters.add("Todas");
        filters.addAll(Arrays.asList(CLASSIFICATIONS));
        searchClassification.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters));
        classification.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Arrays.asList(CLASSIFICATIONS)));
    }

    private void configureActions() {
        searchName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshList(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        searchClassification.setOnItemSelectedListener(new SimpleItemSelectedListener(this::refreshList));
        findViewById(R.id.new_company_button).setOnClickListener(view -> clearForm());
        findViewById(R.id.cancel_button).setOnClickListener(view -> clearForm());
        findViewById(R.id.save_button).setOnClickListener(view -> saveCompany());
    }

    private void saveCompany() {
        String companyName = name.getText().toString().trim();
        if (companyName.isEmpty()) {
            name.setError("Escribe el nombre de la empresa");
            return;
        }
        Company company = editingCompany == null ? new Company() : editingCompany;
        company.name = companyName;
        company.url = url.getText().toString().trim();
        company.phone = phone.getText().toString().trim();
        company.email = email.getText().toString().trim();
        company.products = products.getText().toString().trim();
        company.classification = classification.getSelectedItem().toString();
        database.save(company);
        Toast.makeText(this, editingCompany == null ? "Empresa creada" : "Empresa actualizada", Toast.LENGTH_SHORT).show();
        clearForm();
        refreshList();
    }

    private void refreshList() {
        if (companyList == null) return;
        companyList.removeAllViews();
        String filter = searchName == null ? "" : searchName.getText().toString().trim();
        String selected = searchClassification == null || searchClassification.getSelectedItem() == null
                ? "Todas" : searchClassification.getSelectedItem().toString();
        for (Company company : database.find(filter, selected)) {
            companyList.addView(companyRow(company));
        }
        if (companyList.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No hay empresas que coincidan con el filtro.");
            empty.setPadding(0, 18, 0, 18);
            companyList.addView(empty);
        }
    }

    private View companyRow(Company company) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.company_card);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        TextView title = new TextView(this);
        title.setText(company.name);
        title.setTextColor(0xff0f172a);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView classificationLabel = detailText(company.classification, 0xff2563eb, 13);
        classificationLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(classificationLabel);

        TextView contact = detailText("Web: " + emptyAsDash(company.url) + "\n" +
                "Tel: " + emptyAsDash(company.phone) + "   |   Correo: " + emptyAsDash(company.email),
                0xff475569, 14);
        LinearLayout.LayoutParams contactParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        contactParams.setMargins(0, dp(8), 0, 0);
        contact.setLayoutParams(contactParams);
        card.addView(contact);

        TextView productsLabel = detailText("Servicios: " + emptyAsDash(company.products), 0xff334155, 14);
        LinearLayout.LayoutParams productsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        productsParams.setMargins(0, dp(6), 0, 0);
        productsLabel.setLayoutParams(productsParams);
        card.addView(productsLabel);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(android.view.Gravity.END);
        Button edit = new Button(this);
        edit.setText("Editar");
        edit.setAllCaps(false);
        edit.setTextColor(0xff2563eb);
        edit.setBackgroundResource(R.drawable.secondary_button);
        edit.setOnClickListener(view -> editCompany(company));
        Button delete = new Button(this);
        delete.setText("Eliminar");
        delete.setAllCaps(false);
        delete.setTextColor(0xffB91C1C);
        delete.setOnClickListener(view -> confirmDelete(company));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
        editParams.setMargins(0, dp(10), dp(6), 0);
        edit.setLayoutParams(editParams);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42));
        deleteParams.setMargins(0, dp(10), 0, 0);
        delete.setLayoutParams(deleteParams);
        actions.addView(edit);
        actions.addView(delete);
        card.addView(actions);
        return card;
    }

    private TextView detailText(String text, int color, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String emptyAsDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private void editCompany(Company company) {
        editingCompany = company;
        formTitle.setText("Editar empresa");
        name.setText(company.name);
        url.setText(company.url);
        phone.setText(company.phone);
        email.setText(company.email);
        products.setText(company.products);
        for (int i = 0; i < CLASSIFICATIONS.length; i++) {
            if (CLASSIFICATIONS[i].equals(company.classification)) classification.setSelection(i);
        }
        name.requestFocus();
    }

    private void confirmDelete(Company company) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar empresa")
                .setMessage("¿Quieres eliminar " + company.name + " del directorio?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    database.delete(company.id);
                    refreshList();
                    Toast.makeText(this, "Empresa eliminada", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void clearForm() {
        editingCompany = null;
        formTitle.setText("Agregar empresa");
        name.setText("");
        url.setText("");
        phone.setText("");
        email.setText("");
        products.setText("");
        classification.setSelection(0);
    }
}
