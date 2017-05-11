import java.util.*;


import java.io.*;
import java.net.*;

public class FTPServer {
	public static File dir = new File("/home/selcuk-ubuntu/Desktop/CLOUD");
	
	// UNIX 'ls' komut implementasyonu
	public static String Jls(File ex){
        String childs[] = ex.list();
        String ls="";
        
        for(String child: childs){
        	ls = ls+"\n" + child;
        }
        return ls;
	}
	// UNIX 'pwd' komut implementasyonu
	public static String Jpwd(File ex){
		String pwd = ex.getAbsolutePath();
        return pwd;
	}
	// UNIX 'cd' komut implementasyonu
	public static String Jcd(String path){
		String way=path.trim();
		File tdir;		
		
		if(way.compareTo("..")==0){
			if(dir.getParentFile() == null){
				return Jpwd(dir);
			}
			else{
				dir = dir.getParentFile();
				return Jpwd(dir);
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
			return Jpwd(dir);
		}
		else{
			return path +" bir dizin değil.";
		}
		
	}
	// UNIX 'mkdir' komut implementasyonu
	public static String Jmkdir(String name){			
		if(new File(name).isDirectory() == false){
			new File(name).mkdir();
			return Jls(dir);
		}
		else{
			return name + "klasörü zaten var.";
		}
	}
	
	public static void main(String args[]) throws Exception{
		// ServerSocket ile 21 nolu portta sunucu soketi açılır.
		ServerSocket ssoc = new ServerSocket(21);
		// Aynı anda birden fazla istemci bağlanması durumda istemcilere numara atamak için clientnumber değişkeni kullanılır.
		int clientnumber =1;
		System.out.println("FTP sunucusu 21 nolu portta başlatıldı.");
		try {
            while (true) {// main thread sürekli olarak yeni bir istemci bekler.
            	System.out.println("Yeni bağlantı bekleniyor.");
            	// Yeni istemci geldiğinde, yeni bir thread ile ServerThread objesi oluşturulur ve
                new ServerThread(ssoc.accept(), clientnumber++);
            }
        } finally {
        	// Sunucu kapatılırken 
        	ssoc.close();
        }
	}
	
	// Sunucuya bağlanan her istemci için bu sınıftan bir tane oluşturulur
	// ve o istemcinin socket objesi bu sınıfta işlem görür.
	static class ServerThread extends Thread{
		Socket nsoc;
		int clientnumber = 0;
		DataInputStream din;// soketten gelen veriler bu streamle alınır.
	    DataOutputStream dout;// sokete bu stream kullanılarak veri aktarılır.
	    InetAddress address;// klavyeden gelicek komutlar bu reader ile okunur.
		
		public ServerThread(Socket soc, int number) throws IOException {
			clientnumber = number;// istemcinin numarası
			nsoc = soc;
			din=new DataInputStream(nsoc.getInputStream());// getInputStream atanır.
            dout=new DataOutputStream(nsoc.getOutputStream());// getOutputStream atanır.
            // yeni istemcinin hangi IP ve porttan bağlandığı bilgisi loglanır.
            System.out.println("Yeni istemci "+ nsoc.getInetAddress()+" adresinden "+nsoc.getPort()+" nolu porta bağlanmıştır.");
            dout.writeUTF(clientnumber +" numaralı istemci bağlantın başarılı.");// istemciye bağlantı bilgisi gönderilir.
            dout.writeUTF(Jpwd(dir));// istemciye sunucunun dizini yollanır.
            address = nsoc.getInetAddress();// istemcinin bağlı olup olmadığı buradan kontrol edilecek.
           
			start();
		}
		
		public void run(){
			int timeout=0;// istemcinin abnormal şekildeki kopmalarını tespit etmek için.
			while(timeout < 4)// eğer timeout değeri 4 ve üzeri bir değere çıkarsa istemciyle bağlantı sonlandırılır.
	        {
	            try
	            {	            	
	            	System.out.println(clientnumber + ": Nolu İstemciden Komut bekleniyor ...");
	            	timeout++;//Her bir komut bekleniyor yazısında timeout arttrılır. En fazla 4 kere komut bekleniyor yazısı görülür.
	            	String Command="";
	            	// eğer istemci herhanagi bir veri gönderdiyse timeout her seferinde sıfırlanır.
		            Command=din.readUTF();
		            
		            if(Command.compareTo("DOWN")==0)// istemciden dosya istenince Upload() foksiyonuna yönlendirilir.
		            {
		            	timeout=0;
		                System.out.println(clientnumber+" nolu istemci sunucudan dosya istiyor.");
		                Upload();
		                continue;
		            }
		            else if(Command.compareTo("UP")==0)// istemci dosya göndermek isteyince Download() fonksiyonuna yönlendirilir.
		            {
		            	timeout=0;
		                System.out.println(clientnumber +" nolu istemci sunucuya dosya yüklemek istiyor.");                
		                Download();
		                continue;
		            }
		            else if(Command.compareTo("EXIT")==0)// çıkış işlemleri gerçekleştirilir, socket objesi kapatılır.
		            {
		            	timeout=0;
		                System.out.println("\tİstemciden çıkış emri geldi.");
		                System.out.println(clientnumber+ " nolu"+ nsoc.getInetAddress()+ " adresli istemci çıkış yapmıştır.");
		                nsoc.close();
		                break;
		            }
		            else if(Command.compareTo("ls")==0){// istemciye dizindeki dosyalar gösterilir
		            	timeout=0;
		            	System.out.println(clientnumber +": "+ Command);
		            	String answer = Jls(dir);
		            	dout.writeUTF(answer);		            	
		            }
		            else if(Command.compareTo("pwd")==0){// istemciye dizin yolu gönderilir.
		            	timeout=0;
		            	System.out.println(clientnumber +": "+ Command);
		            	String answer = Jpwd(dir);
		            	dout.writeUTF(answer);		            	
		            }
		            else if(Command.contains("cd")){// istemcinin geçmek istediği dizine sunucuda geçilir
		            	timeout=0;
		            	System.out.println(clientnumber +": "+ Command);
		            	String temp[] = Command.split(" ", 2);
		            	String answer = temp[1];
		            	
		            	dout.writeUTF(Jcd(answer));
		            }
		            else if(Command.contains("mkdir")){// sunucudaki dizinde yeni bir klasör oluşturulur
		            	timeout=0;
		            	System.out.println(clientnumber +": "+ Command);
		            	String temp[] = Command.split(" ", 2);
		            	String answer = temp[1];
		            	
		            	dout.writeUTF(Jmkdir(answer));
		            }
		            // istemci ping kontrolüyle kontrol edilir. Kopma olduyla bağlantı sonlandırılır.
		            if(address.isReachable(1000)!=true || timeout > 3){	            			            		
	            		break;
	            	}
		            
		            
		            
	            }
	            catch(Exception ex)
	            {
	            }	                        
				
	        }
			// istemciyle bağlantı sonlanınca socket kapatmak ve loglamak gibi gerekli işlemler yapılır.
			System.out.println(clientnumber+ " nolu "+ nsoc.getInetAddress()+ " adresli istemciyle bağlantı kesildi.");
            try {
				nsoc.close();
				this.interrupt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Sunucudan istemciye dosya gönderilir
		void Upload() throws Exception
	    {        
	        String filename=din.readUTF();
	        // sunucunun  bulunduğu dizinden dosya alınır.
	        File f=new File(dir.getPath()+"/"+filename);
	        System.out.println(clientnumber + " nolu istemci suncudan " + filename + " dosyasını istedi.");
	        //Dosyanın olup olmadığı kontrol edilir.
	        if(!f.exists())
	        {
	        	//Dosya yoksa istemciye bildirilir.
	            dout.writeUTF("YOK");
	            System.out.println(clientnumber + " nolu istemciye suncudan " + filename + " dosyası olmadığı için gönderilemedi.");
	            return;
	        }
	        else
	        {
	        	// dosyanın var olduğu istemciye bildirilir.
	            dout.writeUTF("VAR");
	            // istemcinin hazır olup olmadığı bilgisi alınır.
	            String status=din.readUTF();
	            if(status!="IPTAL"){	            	
	            	System.out.println("Dosya gönderiliyor...");
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
		            dout.writeUTF("Dosya başarıyla indirildi.");
		            System.out.println(clientnumber + " nolu istemciye suncudan " + filename + " dosyası başarıyla gönderildi.");
	            }
	            else{
	            	System.out.println(clientnumber + " nolu istemciye suncudan " + filename + " dosyası gönderilemedi.");
	            }
	                                        
	        }
	    }
	    
		// İstemciden sunucuya dosya indirilir
	    void Download() throws Exception
	    {
	    	// istemcinin yüklemek istediği dosya ismi alınır.
	        String filename=din.readUTF();
	        // gerekli dosya istenilen isimle oluşturulur.
	        System.out.println(clientnumber + " nolu istemci sunucuya " + filename + " dosyasını göndermek istedi.");
	        // istemcide göndermek istediği dosya yoksa işlem iptal edilir.
	        if(filename.compareTo("YOK")==0)
	        {
	        	System.out.println("Dosya gönderilmedi.");
	            return;
	        }
	        
	        File f=new File(dir.getPath()+"/"+filename);
	        String option;
	        
	        if(f.exists())
	        {
	        	// eğer dosya zaten sunucuda varsa istemcinin üstüne yazmak isteyip, isteymeyeceği sorgulanır.
	            dout.writeUTF("Dosya zaten var.");
	            option=din.readUTF();
	        }
	        else
	        {
	        	// dosya sunucuda halihazırda yoksa hazır olunduğu bilgisi istemciye iletilir.
	            dout.writeUTF("HAZIR");
	            option="E";
	        }
	            
	            if(option.compareTo("E")==0)
	            {	            	
	            	System.out.println("Dosya alınıyor ...");
	            	/// Fileinputstream.write() fonksiyonu ile sunucudan gelen integer cinsindeki değerler
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
	                // dosyanın başarıyla alındığı bilgisi istemciye bildirilir.
	                dout.writeUTF("Dosyası başarıyla sunucuya yüklendi.");
	                System.out.println(filename+ " dosyası başarı ile sunucuya yüklendi.");
	            }
	            else
	            {
	            	System.out.println("Dosya alınmadı. Var olan dosya üzerine yazılmadı.");
	                return;
	            }
	            
	    }
	}

}
