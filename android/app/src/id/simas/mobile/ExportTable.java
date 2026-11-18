package id.simas.mobile;

import java.util.*;
import java.math.BigDecimal;
import org.json.*;

/** One typed table shared by PDF and XLSX, with module-specific website columns. */
final class ExportTable {
  String title, context;
  String[] headers;
  final ArrayList<Object[]> rows = new ArrayList<>();
  final ArrayList<Object[]> totals = new ArrayList<>();
  boolean signatures;
  static final BigDecimal ZERO=BigDecimal.ZERO;
  static String s(JSONObject j,String k){return j==null?"":j.optString(k,"");}
  static BigDecimal n(JSONObject j,String k){try{return new BigDecimal(s(j,k));}catch(Exception e){return ZERO;}}
  static String first(String... s){for(String x:s)if(x!=null&&!x.isEmpty())return x;return "";}
  static JSONObject find(MainActivity a,String table,String id){if(id.isEmpty())return null;for(JSONObject j:a.rows(table,false))if(s(j,"id").equals(id))return j;return null;}
  static String[] area(String raw){String rw="",rt="";java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)(RW|RT)\\s*(\\d+)").matcher(raw);while(m.find()){if(m.group(1).equalsIgnoreCase("RW"))rw=m.group(2);else rt=m.group(2);}if(rw.isEmpty()&&rt.isEmpty()){String[] p=raw.split("/",-1);rw=p[0].trim();if(p.length>1)rt=p[1].trim();}return new String[]{rt,rw};}
  static String composition(JSONObject j){BigDecimal count=j.has("package_count")?n(j,"package_count"):BigDecimal.ONE, extra=n(j,"secondary_count");String main=count.subtract(extra).stripTrailingZeros().toPlainString()+" bks × "+(j.has("package_weight_kg")?n(j,"package_weight_kg"):n(j,"weight_kg")).stripTrailingZeros().toPlainString()+" kg";return extra.signum()>0?main+" + "+extra.stripTrailingZeros().toPlainString()+" bks × "+n(j,"secondary_weight_kg").stripTrailingZeros().toPlainString()+" kg":main;}
  static ExportTable create(MainActivity a,String t,List<JSONObject> values,String audience){
    ExportTable e=new ExportTable();
    e.context="Periode Tahun: "+a.yearLabel(a.year)+" · "+values.size()+" catatan";
    switch(t){
      case "zakat_entries":e.title="Laporan Zakat";e.headers=new String[]{"No.","Tanggal","Nama","Desa/Dusun","RW/RT","Nama RT","Arus","Jenis zakat","Fitrah (kg)","Uang (Rp)","Catatan"};break;
      case "qurban_distribution":e.title="Distribusi Qurban · "+(audience.equals("shohibul")?"Shohibul Qurban":audience.equals("penerima")?"Penerima Qurban":"Keduanya");e.headers=audience.equals("shohibul")?new String[]{"No.","Nama","Jenis Qurban","Alokasi (Bks)","Rincian Bungkus","Berat (Kg)","Catatan"}:audience.equals("penerima")?new String[]{"No.","Wilayah/Dusun","Nama RT","RT","RW","Alokasi (Bks)","Berat (Kg)","Catatan"}:new String[]{"No.","Jenis Penerima","Nama","Jenis Qurban","Wilayah/Dusun","Nama RT","RT","RW","Alokasi (Bks)","Rincian Bungkus","Berat (Kg)","Catatan"};break;
      case "finance_transactions":e.title="Transaksi Keuangan";e.headers=new String[]{"No.","Tanggal","Kategori","Keterangan","Jenis","Nominal (Rp)"};break;
      case "qurban_cash":e.title="Kas Qurban";e.headers=new String[]{"No.","Tanggal","Kategori","Shohibul / Sumber dana","Keterangan","Pemasukan (Rp)","Pengeluaran (Rp)"};break;
      case "qurban_people":e.title="Shohibul Qurban";e.headers=new String[]{"No.","Nama peserta","Tahun","Jenis qurban","Hewan","Nominal setoran (Rp)"};break;
      case "zakat_people":e.title="Database Jamaah";e.headers=new String[]{"No.","Nama","Jenis","Desa/Dusun","RW/RT","Nama RT","Asnaf","Anggota keluarga","Telepon","Alamat"};break;
      case "assets":e.title="Inventaris";e.headers=new String[]{"No.","Nama barang","Jumlah","Kondisi","Lokasi"};break;
      case "mosque_officers":e.title="Pengurus Masjid";e.headers=new String[]{"No.","Nama","Jabatan","Telepon","Alamat"};break;
      case "events":e.title="Kegiatan";e.headers=new String[]{"No.","Kegiatan","Deskripsi","Mulai","Selesai","Status"};break;
      default:throw new IllegalArgumentException("Ekspor modul belum tersedia: "+t);
    }
    BigDecimal kgIn=ZERO,kgOut=ZERO,rpIn=ZERO,rpOut=ZERO,count=ZERO,total=ZERO;
    boolean incoming=false,outgoing=false;int no=0;
    for(JSONObject j:values){no++;Object[] row;
      switch(t){
        case "zakat_entries":{
          JSONObject p=find(a,"zakat_people",s(j,"party_id"));boolean in=s(j,"direction").equals("receipt");
          row=new Object[]{no,s(j,"occurred_on"),first(s(j,"party_name"),s(p,"full_name")),first(s(j,"village"),s(p,"village"),s(p,"address")),first(s(j,"rt_rw"),s(p,"rt_rw")),first(s(j,"rt_name"),s(p,"rt_name")),in?"Penerimaan":"Penyaluran",a.label(s(j,"zakat_type")),n(j,"rice_kg"),n(j,"amount"),s(j,"notes")};
          if(in){incoming=true;kgIn=kgIn.add(n(j,"rice_kg"));rpIn=rpIn.add(n(j,"amount"));}else{outgoing=true;kgOut=kgOut.add(n(j,"rice_kg"));rpOut=rpOut.add(n(j,"amount"));}break;
        }
        case "qurban_distribution":{
          JSONObject p=find(a,"qurban_people",s(j,"shohibul_id"));String name=first(s(j,"recipient"),s(p,"full_name"),"—"),cat=first(a.label(s(p,"qurban_category")),"Arsip lama"),village=first(s(j,"village"),s(j,"address"),"Arsip lama");String[] area=area(s(j,"rt_rw"));BigDecimal c=j.has("package_count")?n(j,"package_count"):BigDecimal.ONE,k=n(j,"weight_kg");boolean sho=s(j,"recipient_kind").equals("shohibul");
          row=audience.equals("shohibul")?new Object[]{no,name,cat,c,composition(j),k,s(j,"notes")}:audience.equals("penerima")?new Object[]{no,village,s(j,"rt_name"),area[0],area[1],c,k,s(j,"notes")}:new Object[]{no,sho?"Shohibul Qurban":"Masyarakat",sho?name:"—",sho?cat:"—",sho?"—":village,sho?"—":s(j,"rt_name"),sho?"—":area[0],sho?"—":area[1],c,composition(j),k,s(j,"notes")};count=count.add(c);kgIn=kgIn.add(k);break;
        }
        case "finance_transactions":{
          JSONObject c=find(a,"finance_categories",s(j,"category_id"));boolean in=s(j,"kind").equals("income");row=new Object[]{no,s(j,"occurred_on"),s(c,"name"),s(j,"description"),in?"Pemasukan":"Pengeluaran",n(j,"amount")};if(in)rpIn=rpIn.add(n(j,"amount"));else rpOut=rpOut.add(n(j,"amount"));break;
        }
        case "qurban_cash":{boolean in=s(j,"kind").equals("income");String raw=s(j,"description"),category=in?(s(j,"id").startsWith("auto-")?"Setoran Shohibul Qurban":"Penerimaan arsip"):"Pengeluaran Lainnya",who=in?first(s(find(a,"qurban_people",s(j,"person_id")),"full_name"),"—"):"Kas Qurban",description=raw;
          if(in&&raw.startsWith("[Operasional Shohibul:")){int end=raw.indexOf("] ");if(end>0)try{JSONObject info=new JSONObject(raw.substring(end+2));category="Penerimaan Operasional · "+raw.substring(21,end);who=s(info,"nama");description=s(info,"catatan");}catch(Exception ignored){}}
          if(!in)for(String c:new String[]{"Pembelian Hewan","Biaya Operasional","Pengeluaran Lainnya"})if(raw.startsWith("["+c+"] ")){category=c;description=raw.substring(c.length()+3);}
          row=new Object[]{no,s(j,"occurred_on"),category,who,description,in?n(j,"amount"):ZERO,in?ZERO:n(j,"amount")};if(in)rpIn=rpIn.add(n(j,"amount"));else rpOut=rpOut.add(n(j,"amount"));break;}
        case "qurban_people":row=new Object[]{no,s(j,"full_name"),s(j,"year"),s(j,"qurban_category").equals("sapi_patungan")?"Rombongan":"Pribadi",a.label(s(j,"qurban_category")),n(j,"nominal")};total=total.add(n(j,"nominal"));break;
        case "zakat_people":row=new Object[]{no,s(j,"full_name"),s(j,"person_type"),first(s(j,"village"),s(j,"address")),s(j,"rt_rw"),s(j,"rt_name"),a.label(s(j,"asnaf")),n(j,"household"),s(j,"phone"),s(j,"address")};break;
        case "assets":row=new Object[]{no,s(j,"name"),n(j,"quantity"),s(j,"condition"),s(j,"location")};total=total.add(n(j,"quantity"));break;
        case "mosque_officers":row=new Object[]{no,s(j,"full_name"),s(j,"position"),s(j,"phone"),s(j,"address")};break;
        default:row=new Object[]{no,s(j,"title"),s(j,"description"),s(j,"starts_at"),s(j,"ends_at"),j.optBoolean("is_published")?"Dipublikasikan":"Draf"};
      }e.rows.add(row);
    }
    if(t.equals("zakat_entries")){
      if(incoming||!outgoing)e.addTotal("TOTAL PENERIMAAN",8,kgIn,9,rpIn);
      if(outgoing)e.addTotal("TOTAL PENYALURAN",8,kgOut,9,rpOut);
      if(incoming&&outgoing)e.addTotal("SALDO",8,kgIn.subtract(kgOut),9,rpIn.subtract(rpOut));
    }else if(t.equals("qurban_distribution"))e.addTotal("TOTAL KESELURUHAN",Arrays.asList(e.headers).indexOf("Alokasi (Bks)"),count,Arrays.asList(e.headers).indexOf("Berat (Kg)"),kgIn);
    else if(t.equals("finance_transactions")){e.addTotal("TOTAL PEMASUKAN",5,rpIn,-1,ZERO);e.addTotal("TOTAL PENGELUARAN",5,rpOut,-1,ZERO);e.addTotal("SALDO",5,rpIn.subtract(rpOut),-1,ZERO);}
    else if(t.equals("qurban_cash")){e.addTotal("TOTAL",5,rpIn,6,rpOut);e.addTotal("SALDO",5,rpIn.subtract(rpOut),-1,ZERO);}
    else if(t.equals("qurban_people"))e.addTotal("TOTAL",5,total,-1,ZERO);
    else if(t.equals("assets"))e.addTotal("TOTAL",2,total,-1,ZERO);
    return e;
  }
  void addTotal(String label,int c,BigDecimal n,int d,BigDecimal m){Object[] row=new Object[headers.length];Arrays.fill(row,"");row[1]=label;if(c>=0)row[c]=n;if(d>=0)row[d]=m;totals.add(row);}
  ExportTable pivot(String group){
    int g=Arrays.asList(headers).indexOf(group);if(g<0)throw new IllegalArgumentException("Kelompok tidak tersedia");
    ArrayList<Integer> measures=new ArrayList<>();for(int i=0;i<headers.length;i++)if(headers[i].contains("(kg)")||headers[i].contains("(Kg)")||headers[i].contains("(Rp)")||headers[i].contains("(Bks)"))measures.add(i);
    ExportTable p=new ExportTable();p.title="Rekap Data · "+title;p.context=context+" · Kelompok: "+group;p.signatures=true;p.headers=new String[3+measures.size()];p.headers[0]="No.";p.headers[1]=group;p.headers[2]="Jumlah catatan";for(int i=0;i<measures.size();i++)p.headers[i+3]=headers[measures.get(i)];
    TreeMap<String,Object[]> groups=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for(Object[] r:rows){String key=String.valueOf(r[g]).trim();if(key.isEmpty())key="Belum diisi";Object[] z=groups.get(key);if(z==null){z=new Object[p.headers.length];z[1]=key;for(int i=2;i<z.length;i++)z[i]=ZERO;groups.put(key,z);}z[2]=((BigDecimal)z[2]).add(BigDecimal.ONE);for(int i=0;i<measures.size();i++)z[i+3]=((BigDecimal)z[i+3]).add((BigDecimal)r[measures.get(i)]);}
    Object[] sum=new Object[p.headers.length];sum[0]="";sum[1]="TOTAL";for(int i=2;i<sum.length;i++)sum[i]=ZERO;int no=0;for(Object[] r:groups.values()){r[0]=++no;p.rows.add(r);for(int i=2;i<sum.length;i++)sum[i]=((BigDecimal)sum[i]).add((BigDecimal)r[i]);}p.totals.add(sum);return p;
  }
}
