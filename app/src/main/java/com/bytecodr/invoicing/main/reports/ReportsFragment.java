package com.bytecodr.invoicing.main.reports;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bytecodr.invoicing.BuildConfig;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.helper.helper_number;
import com.bytecodr.invoicing.main.LoginActivity;
import com.bytecodr.invoicing.main.SettingActivity;
import com.bytecodr.invoicing.network.ErrorResponse;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;
import static com.bytecodr.invoicing.main.LoginActivity.SESSION_USER;
import static com.itextpdf.text.pdf.ColumnText.AR_LIG;

public class ReportsFragment
        extends Fragment
        implements ReportsFragmentModelListener, View.OnClickListener {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd_MM_yyyy", Locale.getDefault());

    private static final int SIZE_TEXT_TITLE_PAGE = 22;
    private static final int SIZE_TEXT_HEADER_PAGE = 14;
    private static final int SIZE_TEXT_HEADER_TABLE = 14;
    private static final int SIZE_TEXT_TOTAL = 14;
    private static final int SIZE_TEXT_NORMAL = 14;

    private String currentReportTitle;

    /**
     * REPORT SETUP
     **/
    private String logoImage;
    private BaseFont bfBold;
    private BaseFont bf;
    private int pageNumber = 0;
    private double Subtotal;
    private double Tax;

    private Date invoiceDate = null;
    private Date invoiceDueDate = null;

    private List<Item> mInvoice;
    private List<Item> mPurchase;

    private ReportsFragmentModel mModel;

    MaterialDialog progressDialog;

    private TextView tvInvoiceSum;
    private TextView tvPurchaseSum;
    private TextView tvDifference;
    private TextView tvDate;
    private Button btnDownloadPdf;
    private ViewPager vp;
    private TabLayout tl;

    public ReportsFragment() {}

    public static ReportsFragment newInstance() {
        ReportsFragment fragment = new ReportsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }

        mInvoice = new ArrayList<>();
        mPurchase = new ArrayList<>();
        mModel = new ReportsFragmentModel(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews();
        createProgressDialog();
        loadData();
    }

    private void createProgressDialog() {
        progressDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();
    }

    private void initViews() {
        View view = getView();
        tvInvoiceSum = (TextView) view.findViewById(R.id.tvInvoiceSum);
        tvPurchaseSum = (TextView) view.findViewById(R.id.tvPurchaseSum);
        tvDifference = (TextView) view.findViewById(R.id.tvDifference);
        tvDate = (TextView) view.findViewById(R.id.tvDate);
        btnDownloadPdf = (Button) view.findViewById(R.id.btnDownloadPdf);
        btnDownloadPdf.setOnClickListener(this);
        vp = (ViewPager) view.findViewById(R.id.vp);
        tl = (TabLayout) view.findViewById(R.id.tl);
    }

    private void loadData() {
        showDialog();

        SharedPreferences settings = getActivity().getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);
        int userId = settings.getInt("id", -1);
        if (userId!=-1) {
            mModel.getReport(userId);
        }
        tvDate.setText(dateFormat.format(new Date()));
    }



    /* BEG MODEL CALLBACKS */

    @Override
    public void onGetReportSuccess(double invoiceSum, double purchaseSum, double difference, List
                <Item> invoices, List<Item> purchases) {
        mInvoice = invoices;
        mPurchase = purchases;

        Activity activity = getActivity();
        SharedPreferences settings = activity.getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);
        String currencySymbol = settings.getString(SettingActivity.KEY_CURRENCY_SYMBOL, "$");
        tvInvoiceSum.setText(currencySymbol + " " + String.valueOf(invoiceSum));
        tvPurchaseSum.setText(currencySymbol + " " + String.valueOf(purchaseSum));
        tvDifference.setText(currencySymbol + " " + String.valueOf(difference));

        PagerAdapter adapter = new PagerAdapter(getChildFragmentManager(), invoices, purchases);
        vp.setAdapter(adapter);
        tl.setupWithViewPager(vp);

        hideDialog();
    }

    @Override
    public void onGetReportFailure(ErrorResponse response) {
        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
        hideDialog();
    }

    private void showDialog() {
        if (progressDialog!=null && !progressDialog.isShowing())
            progressDialog.show();
    }

    private void hideDialog() {
        if (progressDialog!=null && progressDialog.isShowing())
            progressDialog.hide();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnDownloadPdf){
            currentReportTitle = "Report_" + dateFormat1.format(new Date());
            String path = downloadPDF();

            if (path.length() == 0) {
                Toast.makeText(getActivity(), getResources().getString(R.string
                        .pdf_not_created), Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", new File(path)), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        }
    }

    /*------------------------------------- Download PDF Begins ----------------------------------*/

    public String downloadPDF() {
        return createPDF(currentReportTitle + ".pdf");
    }

    private Integer address1_start;
    private Integer address2_start;
    private Integer address_height;

    private Integer line_heading_start;
    private Integer line_item_start;
    private Integer line_item_height;

    private BaseColor color_light_grey = new BaseColor(227, 227, 227);
    private BaseColor color_invoice_header_background = new BaseColor(244, 67, 54);

    private String createPDF(String pdfFilename) {

        address1_start = 700;
        address2_start = 700;
        address_height = SIZE_TEXT_HEADER_PAGE + 3;

        line_heading_start = 635;
        line_item_start = 600;
        line_item_height = 28;


        pageNumber = 0;
        Subtotal = 0;
        Tax = 0;
        Document doc = new Document();
        PdfWriter docWriter = null;
        initializeFonts();

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + pdfFilename;

        try {
            //invoiceFile = new File(path);

            docWriter = PdfWriter.getInstance(doc, new FileOutputStream(path));
            docWriter.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            doc.addAuthor("Bytecodr");
            doc.addLanguage("ar-SA");
            doc.addCreationDate();
            doc.addProducer();
            doc.addTitle(currentReportTitle);
            doc.setPageSize(PageSize.LETTER);

            doc.open();
            PdfContentByte cb = docWriter.getDirectContent();

            boolean beginPage = true;
            int y = 0;

            for (int i = 0; i < mInvoice.size(); i++) {
                if (beginPage) {
                    beginPage = false;
                    generateInvoiceLayout(doc, cb);
                    generateHeader(doc, cb);
                    y = line_item_start;
                }
                generateInvoiceDetail(doc, cb, i, y);
                y = y - line_item_height;
                if (y < 50) {
                    printPageNumber(cb);
                    doc.newPage();
                    beginPage = true;
                }
            }


            //This is the last item (notes). If it's over page, put it all on the next page.
            if ((y - 80) < 50) {
                printPageNumber(cb);
                doc.newPage();
                y = address1_start;
            }

            y -= 10;

            createText(cb, 460, y - 10, getResources().getString(R.string.invoice_total),
                    SIZE_TEXT_TOTAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 10, tvInvoiceSum.getText().toString(), SIZE_TEXT_TOTAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);



            /* purchase details */
            for (int i = 0; i < mPurchase.size(); i++) {
                if (i == 0) {
                    line_heading_start = y - 50;
                    generatePurchaseLayout(doc, cb);
                    y = line_heading_start - line_item_height;
                }
                generatePurchaseDetail(doc, cb, i, y);
                y = y - line_item_height;
                if (y < 50) {
                    printPageNumber(cb);
                    doc.newPage();
                }
            }


            //This is the last item (notes). If it's over page, put it all on the next page.
            if ((y - 80) < 50) {
                printPageNumber(cb);
                doc.newPage();
                y = address1_start;
            }

            y -= 10;

            createText(cb, 460, y - 10, getResources().getString(R.string.purchase_total),
                    SIZE_TEXT_TOTAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 10, tvPurchaseSum.getText().toString(), SIZE_TEXT_TOTAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);

            if ((y - 80) < 50) {
                printPageNumber(cb);
                doc.newPage();
                y = address1_start;
            }
            y -= 50;

            createText(cb, 460, y - 10, getResources().getString(R.string.difference),
                    SIZE_TEXT_TOTAL, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 10, tvDifference.getText().toString(), SIZE_TEXT_TOTAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);

            printPageNumber(cb);

        } catch (DocumentException ex) {
            ex.printStackTrace();
            return "";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        } finally {
            if (doc != null) {
                doc.close();
            }
            if (docWriter != null) {
                docWriter.close();
            }
        }

        return path;
    }

    private void generateInvoiceLayout(Document doc, PdfContentByte cb) {

        try {
            Rectangle rec = new Rectangle(30, line_heading_start + 15, 580, line_heading_start - 8);
            rec.setBackgroundColor(color_invoice_header_background);
            cb.rectangle(rec);

            // Invoice Detail box Text Headings
            createText(cb, 40, line_heading_start, getResources().getString(R.string.invoice) +"#", SIZE_TEXT_HEADER_TABLE, bf, BaseColor
                    .WHITE, Element.ALIGN_LEFT);
            createText(cb, 360, line_heading_start, getResources().getString(R.string.amount),
                    SIZE_TEXT_HEADER_TABLE, bf, BaseColor.WHITE, Element.ALIGN_LEFT);
            createText(cb, 460, line_heading_start, getResources().getString(R.string.vat_capitalized),
                    SIZE_TEXT_HEADER_TABLE, bf, BaseColor.WHITE, Element.ALIGN_RIGHT);
            createText(cb, 568, line_heading_start, getResources().getString(R.string.total),
                    SIZE_TEXT_HEADER_TABLE, bf, BaseColor.WHITE, Element.ALIGN_RIGHT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void generatePurchaseLayout(Document doc, PdfContentByte cb) {

        try {
            Rectangle rec = new Rectangle(30, line_heading_start + 15, 580, line_heading_start - 8);
            rec.setBackgroundColor(color_invoice_header_background);
            cb.rectangle(rec);

            // Invoice Detail box Text Headings
            createText(cb, 40, line_heading_start, getResources().getString(R.string.purchase) +"#", SIZE_TEXT_HEADER_TABLE, bf, BaseColor
                    .WHITE, Element.ALIGN_LEFT);
            createText(cb, 360, line_heading_start, getResources().getString(R.string.amount),
                    SIZE_TEXT_HEADER_TABLE, bf, BaseColor.WHITE, Element.ALIGN_LEFT);
            createText(cb, 460, line_heading_start, getResources().getString(R.string.vat_capitalized),
                    SIZE_TEXT_HEADER_TABLE, bf, BaseColor.WHITE, Element.ALIGN_RIGHT);
            createText(cb, 568, line_heading_start, getResources().getString(R.string.total),
                    SIZE_TEXT_HEADER_TABLE, bf, BaseColor.WHITE, Element.ALIGN_RIGHT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void generateHeader(Document doc, PdfContentByte cb) {

        try {

            SharedPreferences settings = getActivity().getSharedPreferences(SESSION_USER, MODE_PRIVATE);

            Integer address_startX = 30;

            createText(cb, address_startX, address1_start, settings.getString("firstname", "") +
                            " " + settings.getString("lastname", ""), SIZE_TEXT_HEADER_PAGE, bf,
                    BaseColor.BLACK, Element.ALIGN_LEFT);



            createText(cb, 350, address1_start, getResources().getString(R.string
                    .report_capitalized), SIZE_TEXT_TITLE_PAGE, bf, BaseColor.BLACK, Element
                    .ALIGN_RIGHT);

            /*if (!logoImage.isEmpty()) {
                Image image = Image.getInstance(Base64.decode(logoImage, Base64.DEFAULT));
                float width = image.getScaledWidth();
                float height = image.getScaledHeight();
                image.setAlignment(Element.ALIGN_RIGHT);
                image.setAbsolutePosition(410 + (180 - width), address1_start - height + 10);
                cb.addImage(image);
            }*/


            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

            createText(cb, 460, address2_start - (address_height * 1) - 5, getResources()
                    .getString(R.string.report_date), SIZE_TEXT_HEADER_PAGE, bf, BaseColor
                    .BLACK, Element.ALIGN_RIGHT);
            createText(cb, 580, address2_start - (address_height * 1) - 5, dateFormat.format
                            (new Date()),
                    SIZE_TEXT_HEADER_PAGE, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void generateInvoiceDetail(Document doc, PdfContentByte cb, int index, int y) {
        Item item = mInvoice.get(index);

        if (item == null) return;

        try {

            createText(cb, 40, y, "INV-" + item.id, SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element
                    .ALIGN_LEFT);
            createText(cb, 400, y, helper_number.round(item.amount), SIZE_TEXT_NORMAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);

            createText(cb, 460, y, helper_number.round(item.vat), SIZE_TEXT_NORMAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y, helper_number.round(item.total), SIZE_TEXT_NORMAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);

            LineSeparator lineSeparator = new LineSeparator();

            lineSeparator.setLineColor(color_light_grey);
            lineSeparator.drawLine(cb, 30, 580, y - 10);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void generatePurchaseDetail(Document doc, PdfContentByte cb, int index, int y) {
        Item item = mPurchase.get(index);

        if (item == null) return;

        try {

            createText(cb, 40, y, "PRC-" + item.id, SIZE_TEXT_NORMAL, bf, BaseColor.BLACK, Element
                    .ALIGN_LEFT);
            createText(cb, 400, y, helper_number.round(item.amount), SIZE_TEXT_NORMAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);

            createText(cb, 460, y, helper_number.round(item.vat), SIZE_TEXT_NORMAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y, helper_number.round(item.total), SIZE_TEXT_NORMAL, bf,
                    BaseColor.BLACK, Element.ALIGN_RIGHT);

            LineSeparator lineSeparator = new LineSeparator();

            lineSeparator.setLineColor(color_light_grey);
            lineSeparator.drawLine(cb, 30, 580, y - 10);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void createText(PdfContentByte cb, float x, float y, String text, Integer size,
                            BaseFont font, BaseColor color, Integer alignment) {
        Font fnt = new Font(font, size);
        fnt.setColor(color);
        Phrase phrase = new Phrase(text, fnt);

        ColumnText.showTextAligned(cb, alignment, phrase, x, y, 0, PdfWriter.RUN_DIRECTION_RTL,AR_LIG);

    }

    private void printPageNumber(PdfContentByte cb) {

        cb.beginText();

        cb.setFontAndSize(bfBold, 8);
        cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, getResources().getString(R.string.page_no)
                + (pageNumber + 1), 570, 25, 0);
        cb.endText();

        pageNumber++;
    }

    private void initializeFonts() {
        try {

            bfBold = BaseFont.createFont("res/font/tradbdo.ttf", BaseFont.IDENTITY_H,BaseFont.EMBEDDED);
            bf = BaseFont.createFont("res/font/trado.ttf", BaseFont.IDENTITY_H,BaseFont.EMBEDDED);

        } catch (DocumentException ex) {
            //ex.printStackTrace();
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
    }
    /*-------------------------------------- Download PDF End ------------------------------------*/
}
