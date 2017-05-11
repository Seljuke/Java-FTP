// FTP Client

import java.net.*;
import java.io.*;
import java.util.*;


public class FTPClient
{
	// İSTEMCİNİN BAŞLANGIÇ DİZİNİ
public static File dir = new File("user.dir");
public static String Sdir; // Bağlanılan sunucunun dizini
	
	// UNIX 'ls' komut implementasyonu
	public static String Jls(){				
        String childs[] = dir.list();
        String ls="";
        
        
        for(String child: childs){
        	ls = ls+"\n" + child;
        }
        return ls;
	}
	// UNIX 'pwd' komut implementasyonu
	public static String Jpwd(){
		String pwd = dir.getAbsolutePath();
        return pwd;
	}
	// UNIX 'cd' komut implementasyonu
	public static String Jcd(String path){
		String way=path.trim();
		File tdir;		
		
		if(way.compareTo("..")==0){
			if(dir.getParentFile() == null){
				return Jpwd();
			}
			else{
				dir = dir.getParentFile();
				return Jpwd();
			}			
		}
		if(way.contains(dir.getAbsolutePath()) == false){
			tdir = new File(dir.getAbsolutePath()+"/"+way);
		}
		else{
			tdir = new File(way);
		}	
		
		if(tdir.isDirectory()==true){
			dir = tdir;
			return Jpwd();
		}
		else{
			return path +" bir dizin değil.";
		}
		
	}
	// UNIX 'mkdir' komut implementasyonu
	public static String Jmkdir(String name){			
		if(new File(name).isDirectory() == false){
			new File(name).mkdir();
			return Jls();
		}
		else{
			return name + "klasörü zaten var.";
		}
	}
	
	
    public static void main(String args[]) throws Exception
    {
    	String dirway = Jpwd(); // İstemci dizininin yolunun sonundaki 'user.dir' yazısı silinir.
    	if(dirway.contains("user.dir")){
    		dirway = dirway.replaceAll("user.dir", "");
    	}
    	dir = new File(dirway);
    	// Kullanıcıdan bağlanmak isteiği sunucunun IP si alınır.
    	System.out.println("Bağlanmak istediğiniz sunucunun IP adresini giriniz: ");
    	Scanner ip = new Scanner(System.in);
    	// IP si belli sunucunun 21 nolu portuna bağlanır.
        Socket soc=new Socket(ip.next(),21);
        // İstemcinin komutlarını işleyecek sınıf başlatılır.
        transferfileClient t=new transferfileClient(soc);
        t.displayMenu();
        
    }
    
    
    static class transferfileClient
    {
        Socket ClientSoc;

        DataInputStream din;// soketten gelen veriler bu streamle alınır.
        DataOutputStream dout;// sokete bu stream kullanılarak veri aktarılır.
        BufferedReader br;// klavyeden gelicek komutlar bu reader ile okunur.
        public transferfileClient(Socket soc)
        {
            try
            {
                ClientSoc=soc;
                din=new DataInputStream(ClientSoc.getInputStream());
                dout=new DataOutputStream(ClientSoc.getOutputStream());
                br=new BufferedReader(new InputStreamReader(System.in));
                // sunucudan baplantının başarılı olduğu ve istemci numarası bilgisi alınır.
                System.out.println(din.readUTF()); 
                Sdir = din.readUTF();// ilk olarak sunucuya bağlanıldığı andaki dizin yolu alınır.
            }
            catch(Exception ex)
            {
            }        
        }
        
        // İstemciden sunucuya dosya gönderilir
        void Upload() throws Exception
        {        
            
            String filename;
            System.out.print("Dosya adı giriniz :");
            filename=br.readLine();
            // istemcinin  bulunduğu dizinden dosya alınır.    
            File f=new File(dir.getPath()+"/"+filename);
            if(!f.exists())
            {
                System.out.println("Böyle bir dosya bulunmamaktadır.");
                dout.writeUTF("YOK");
                return;
            }
            // Gönderilecek dosyanın ismi sunucuya iletilir.
            dout.writeUTF(filename);
            // Sunucudan dosyanın var olup olmadığı bilgisi alınır.
            String msgFromServer=din.readUTF();
            if(msgFromServer.compareTo("Dosya zaten var.")==0)
            {
                String Option;
                System.out.println("Dosya zaten var. Üstüne yazmak ister misiniz (E/H)?");
                Option=br.readLine().toUpperCase();            
                if(Option=="E")    
                {
                    dout.writeUTF("E");
                }
                else
                {
                    dout.writeUTF("H");
                    return;
                }
            }
            
            System.out.println("Dosya gönderiliyor ...");
            // Fileinputstream.read() foksiyonuile dosya integer değerler halinde okunur,
            // okunan değerler -1 ise dosya sonuna gelinmiş demektir.
            // -1 gelene kadar dosya okunur ve gönderilir.
            FileInputStream fin=new FileInputStream(f);
            int ch;
            do
            {
                ch=fin.read();
                dout.writeUTF(String.valueOf(ch));
            }
            while(ch!=-1);
            fin.close(); // Fileinputstream yani dosya kapatılıp serbest bırakılır.
            // Sunucudan dosya gönderiminin başarılı olduğu bilgisi alınır.
            System.out.println(din.readUTF());
            
        }
        
        //Sunucudan dosya indirilir
        void Download() throws Exception
        {
            String fileName;
            System.out.print("Dosya adı giriniz :");
            fileName=br.readLine();
            // Sunucuya indirilmek istenen dosyanın adı gönderilir.
            dout.writeUTF(fileName);
            // Sunucudan dosyanın durumu alınır.
            String msgFromServer=din.readUTF();
            
            
            if(msgFromServer.compareTo("YOK")==0)
            {
                System.out.println("Sunucu'da böyle bir dosya bulunmamaktadır.");
                return;
            }
            else if(msgFromServer.compareTo("VAR")==0)
            {
                System.out.println("Dosya alınıyor ...");
                // File tipindeki 'dir' değişkeninin içine dosya indirilir. 
                File f=new File(dir.getPath()+"/"+fileName);
                // Dosyanın var olup olmadığı ve üstüne yazılmak istenip istenmedi sorgulanır.
                if(f.exists())
                {
                    String Option;
                    System.out.println("Dosya zaten var. Üstüne yazmak ister misiniz (E/H)?");
                    Option=br.readLine().toUpperCase();            
                    if(Option=="H")    
                    {
                    	dout.writeUTF("IPTAL");
                        dout.flush();
                        return;    
                    }                
                }
                else{
                	dout.writeUTF("HAZIR");
                	// Fileinputstream.write() fonksiyonu ile sunucudan gelen integer cinsindeki değerler
                	// File tipindeki f değişkenine yazılır.
                    FileOutputStream fout=new FileOutputStream(f);
                    int ch;
                    String temp;
                    do
                    {
                        temp=din.readUTF();
                        ch=Integer.parseInt(temp);
                        if(ch!=-1)
                        {
                            fout.write(ch);                    
                        }
                    }while(ch!=-1);
                    fout.close();
                    // Sunucudan dosya alımının başarılı olduğu bilgisi alınır.
                    System.out.println(din.readUTF());
                }
                
                    
            }
            
            
        }

        public void displayMenu() throws Exception
        {
            while(true)// Kullanıcıdan komutlar burada alınır ve yönlendirilir.
            {               	
            	System.out.print("\n============================\n");
            	// İstemcinin işlem gördüğü dosya dizini
            	System.out.println("İstemci Dizini: "+ Jpwd());
            	// Sunucunun işlem gördüğü sunucu dizini. Herzaman güncel değil.
            	// Güncellemek için sunucuya 'pwd' komutu gönderilmesi yeterli.
            	System.out.println("Sunucu Dizini: "+ Sdir);
                System.out.println("[ MENU ]");
                System.out.println("1. Dosya Gönder");
                System.out.println("2. Dosya İndir");
                System.out.println("3. Sunucu Dosya Gezgini");
                System.out.println("4. İstemci Dosya Gezgini");
                System.out.println("5. Çıkış");
                System.out.print("\nKomut Giriniz :");                
                int choice;
                try{
                	choice=Integer.parseInt(br.readLine());
                } catch (Exception e) {
                	System.out.print("Lütfen bir sayı giriniz."); 
                	continue;
				}
                
                if(choice==1)
                {
                	// Sunucuya dosya gönderileceği bilgisi iletilir.
                    dout.writeUTF("UP");
                    Upload();
                }
                else if(choice==2)
                {
                	// Sunucuya dosya indirileceği bilgisi iletilir.
                    dout.writeUTF("DOWN");
                    Download();
                }
                else if(choice == 5)// Uygulamaya çıkış emri verilir.
                {
                	// Sunucuya istemcinin uygulamayı kapattığı bilgisi iletilir
                    dout.writeUTF("EXIT");
                    System.exit(1);
                }
                else if(choice == 3){// Sunucu dizinindeki işlemler yapılır.
                	int bre=0;
                	while(bre==0){
                		System.out.print("\n============================\n");
                		System.out.println("Suncunun şu anki başlangıç dizinini öğrenmek için 'pwd'");
                		System.out.println("Suncudaki dosyaları listelemek için 'ls'");
                        System.out.println("Suncudaki dizinler arasında gezinmek için 'cd'");
                        System.out.println("Suncuda yeni bir klasör oluşturmak için 'mkdir' komutlarını kullanabilirsiniz.");
                        System.out.println("Menüye dönmek için 'x'");
                        System.out.println("Komut Giriniz :");                        
                        String choice2=br.readLine();
                        choice2.trim();
                        
                        if(choice2.contains("ls")){// UNIX 'ls' komutu sunucu üzerinde uygulanır.
                        	dout.writeUTF(choice2);
                        	String answer = din.readUTF();                        	
                        	System.out.println(answer);
                        }
                        else if(choice2.contains("pwd")){// UNIX 'pwd' komutu sunucu üzerinde uygulanır.
                        	dout.writeUTF(choice2);
                        	String Sdir2 = din.readUTF();
                        	System.out.println(Sdir2);
                        	Sdir=Sdir2;
                        }
                        else if(choice2.contains("cd")){// UNIX 'cd' komutu sunucu üzerinde uygulanır.
                        	dout.writeUTF(choice2);
                        	String Sdir = din.readUTF();
                        	System.out.println(Sdir);                	
                        }
                        else if(choice2.contains("mkdir")){// UNIX 'mkdir' komutu sunucu üzerinde uygulanır.
                        	dout.writeUTF(choice2);
                        	String answer = din.readUTF();
                        	System.out.println(answer);                	
                        }
                        else if(choice2.contains("x")){// Ana menüye geri dönülür
                        	bre=1;
                        }
                	}
                	
                }
                else if(choice == 4){// İstemci dizinindeki işlemler burada yapılır.
                	int bre=0;
                	while(bre==0){
                		System.out.print("\n============================\n");
                		System.out.println("İstemcideki dosyaları listelemek için 'ls'");
                        System.out.println("İstemcideki dizinler arasında gezinmek için 'cd'");
                        System.out.println("İstemcide yeni bir klasör oluşturmak için 'mkdir' komutlarını kullanabilirsiniz.");
                        System.out.println("Menüye dönmek için 'x'");
                        System.out.print("Komut Giriniz :");
                        
                        String choice2=br.readLine();
                        choice2.trim();
                        
                        
                        if(choice2.contains("ls")){// UNIX 'ls' komutu istemci üzerinde uygulanır.
                        	String answer = Jls();
                        	System.out.println(answer);
                        }
                        else if(choice2.contains("cd")){// UNIX 'cd' komutu istemci üzerinde uygulanır.
                        	String temp[] = choice2.split(" ", 2);
    		            	String path = temp[1];
    		            	String answer =Jcd(path);
    		            	System.out.println(answer);
                        }
                        else if(choice2.contains("mkdir")){// UNIX 'mkdir' komutu istemci üzerinde uygulanır.
                        	String temp[] = choice2.split(" ", 2);
    		            	String path = temp[1];
    		            	String answer =Jmkdir(path);
    		            	System.out.println(answer);              	
                        }
                        else if(choice2.contains("x")){
                        	bre=1;
                        }
                	}
                	
                }
                
            }
        }
    }
}


