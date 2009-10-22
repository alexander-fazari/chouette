package fr.certu.chouette.shell;

import chouette.schema.ChouettePTNetworkType;
import fr.certu.chouette.echange.ILectureEchange;
import fr.certu.chouette.service.fichier.IImportateur;
import fr.certu.chouette.service.validation.commun.TypeInvalidite;
import fr.certu.chouette.service.xml.ILecteurEchangeXML;
import fr.certu.chouette.service.xml.ILecteurFichierXML;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;

public class ImportXMLMain {
	
	private static final Logger             logger               = Logger.getLogger(fr.certu.chouette.shell.ImportXMLMain.class);
	private static final String             CHEMIN_CONFIG_SPRING = "applicationContext.xml";
	private static final String             FORMAT_XML           = ".xml";
	private static final String             FORMAT_CSV           = ".csv";
	private static       ILecteurFichierXML lecteurFichierXML;
	private static       ILecteurEchangeXML lecteurEchangeXML;
	private static       IImportateur       importateur;
	private static       Version            version;
	private static       boolean            importer             = true;
	private static       boolean            verify               = true;
	private static       File               logFile              = null;
	private static       File               valFile              = null;
	private static       Set<String>        files                = new HashSet<String>();
	private static       String             format               = null;
	private static final String             JEU_CARACTERES       = "ISO-8859-1"; 

	public static final void main(String[] args) {
		try {
			ImportXMLMain importXMLMain = new ImportXMLMain();
			importXMLMain.importFiles(args);
		}
		catch(Exception e) {
			System.out.println("Probleme fatal : "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void getBeans() {
		ClassPathXmlApplicationContext factory = null;
		try {
			factory = new ClassPathXmlApplicationContext(CHEMIN_CONFIG_SPRING);
		}
		catch(BeansException e) {
			System.out.println("Echec du chargement du context \""+CHEMIN_CONFIG_SPRING+"\"\n"+e.getMessage());
			System.exit(0);
		}
		catch(Exception e) {
			System.out.println("Echec de creation du context \""+CHEMIN_CONFIG_SPRING+"\"\n"+e.getMessage());
			System.exit(1);
		}
		try {
			lecteurFichierXML = (ILecteurFichierXML)factory.getBean("lecteurFichierXML");
		}
		catch(BeansException e) {
			System.out.println("Echec du chargement du Bean \"lecteurFichierXML\"\n"+e.getMessage());
			System.exit(2);
		}
		catch(NestedRuntimeException e) {
			System.out.println("Echec de creation du Bean \"lecteurFichierXML\"\n"+e.getMessage());
			System.exit(3);
		}
		try {
			lecteurEchangeXML = (ILecteurEchangeXML)factory.getBean("lecteurEchangeXML");
		}
		catch(BeansException e) {
			System.out.println("Echec du chargement du Bean \"lecteurEchangeXML\"\n"+e.getMessage());
			System.exit(4);
		}
		catch(NestedRuntimeException e) {
			System.out.println("Echec de creation du Bean \"lecteurEchangeXML\"\n"+e.getMessage());
			System.exit(5);
		}
		try {
			importateur = (IImportateur)factory.getBean("importateur");
		}
		catch(BeansException e) {
			System.out.println("Echec du chargement du Bean \"importateur\"\n"+e.getMessage());
			System.exit(6);
		}
		catch(NestedRuntimeException e) {
			System.out.println("Echec de creation du Bean \"importateur\"\n"+e.getMessage());
			System.exit(7);
		}
		try {
			version = (Version)factory.getBean("version");
		}
		catch(BeansException e) {
			System.out.println("Echec du chargement du Bean \"version\"\n"+e.getMessage());
			System.exit(8);
		}
		catch(NestedRuntimeException e) {
			System.out.println("Echec de creation du Bean \"version\"\n"+e.getMessage());
			System.exit(9);
		}
	}
	
	private void parseArguments(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String opt = null;
			if (args[i].startsWith("--"))
				opt = args[i].substring(2);
			else if (args[i].startsWith("-"))
				opt = args[i].substring(1);
			else {
				files.add(args[i]);
				continue;
			}
			if (opt.equals("h") || opt.equals("help")) {
				printUsage();
				System.exit(0);
			}
			else if (opt.equals("i")) {
				importer = false;
			}
			else if (opt.equals("d") || opt.equals("diretory")) {
				i++;
				if ((i == args.length) || args[i].startsWith("-")) {
					System.out.println("L'argument \""+args[i-1]+"\" necessite un repertoire en option.");
					printUsage();
					System.exit(1);
				}
				String repertoire = args[i];
				if (repertoire != null)
					if (format != null)
						files.addAll(getFiles(repertoire, format));
					else
						files.addAll(getFiles(repertoire, FORMAT_XML));
			}
			else if (opt.equals("X") || opt.equals("xml"))
				if ((format != null) && (format.equals(FORMAT_CSV))) {
					System.out.println("Un seul choix \"XML\" ou \"CSV\"");
					printUsage();
					System.exit(2);
				}
				else
					format = FORMAT_XML;
			else if (opt.equals("C") || opt.equals("csv"))
				if ((format != null) && (format.equals(FORMAT_XML))) {
					System.out.println("Un seul choix \"XML\" ou \"CSV\"");
					printUsage();
					System.exit(3);
				}
				else
					format = FORMAT_CSV;
			else if (opt.startsWith("verify")) {
				if (opt.equals("verify")) {
					i++;
					if ((i == args.length) || args[i].startsWith("-")) {
						System.out.println("L'argument \""+args[i-1]+"\" necessite \"=true\" ou \"=false\" en option.");
						printUsage();
						System.exit(2);
					}
					opt = args[i];
				}
				else
					opt = opt.substring(6);
				if (opt.equals("=")) {
					i++;
					if ((i == args.length) || args[i].startsWith("-")) {
						System.out.println("L'argument \""+args[i-2]+"\" necessite \"= true\" ou \"= false\" en option.");
						printUsage();
						System.exit(3);
					}
					opt = args[i];
				}
				else
					if (opt.startsWith("="))
						opt = opt.substring(1);
					else {
						System.out.println("L'argument \""+args[i-1]+"\" necessite \"= {boolean}\" en option.");
						printUsage();
						System.exit(4);
					}
				if (opt.equals("false"))
					verify = false;
				else
					if (!opt.equals("true")) {
						printUsage();
						System.exit(5);
					}
			}
			else if (opt.equals("logLevel")) {
				i++;
				if ((i == args.length) || args[i].startsWith("-")) {
					printUsage();
					System.exit(6);
				}
				opt = args[i];
				Enumeration loggers = LogManager.getLoggerRepository().getCurrentLoggers();
				while (loggers.hasMoreElements()) {
					if (opt.equals("DEBUG"))
						((Logger)loggers.nextElement()).setLevel(Level.DEBUG);
					else if (opt.equals("INFO"))
						((Logger)loggers.nextElement()).setLevel(Level.INFO);
					else if (opt.equals("WARN"))
						((Logger)loggers.nextElement()).setLevel(Level.WARN);
					else if (opt.equals("ERROR"))
						((Logger)loggers.nextElement()).setLevel(Level.ERROR);
					else if (opt.equals("FATAL"))
						((Logger)loggers.nextElement()).setLevel(Level.FATAL);
					else {
						printUsage();
						System.exit(7);					
					}
					logger.setLevel(Level.DEBUG);
				}
			}
			else if (opt.equals("logFile")) {
				try {
					i++;
					if ((i == args.length) || args[i].startsWith("-")) {
						i--;
						throw new Exception();
					}
					else
						logFile = new File(args[i]);
				}
				catch(Exception e) {
					printUsage();
					System.out.println("L'option \"-logFile\" doit sp�cifier un fichier de log.");
					logFile = null;
				}
				/*
				finally {
					System.out.println("Chouette-ninoxe ne peut pas cr�er le fichier \""+args[i]+"\"");
					System.out.println("Les logs seront affich�es");
					logFile = null;
				}
				 */
			}
			else if (opt.equals("valFile")) {
				try {
					i++;
					if ((i == args.length) || args[i].startsWith("-")) {
						i--;
						throw new Exception();
					}
					else
						valFile = new File(args[i]);
				}
				catch(Exception e) {
					printUsage();
					System.out.println("L'option \"-logFile\" doit sp�cifier un fichier de log.");
					valFile = null;
				}
			}
			else {
				printUsage();
				System.exit(8);
			}
		}
		if (logFile != null) {
			Logger aLogger = Logger.getRootLogger();
			Enumeration appenders = aLogger.getAllAppenders();
			aLogger.removeAllAppenders();
			try {
				aLogger.addAppender(new FileAppender(new SimpleLayout() ,logFile.getCanonicalPath()));
				System.out.println("Les logs seront dans le fichier \""+logFile.getCanonicalPath()+"\"");
			}
			catch (IOException e) {
				while (appenders.hasMoreElements())
					aLogger.addAppender((Appender)appenders.nextElement());
				System.out.println("Echec lors de la creation du fichier de log "+logFile.getName());
				System.out.println("Les logs seront d�rig�s vers les fichiers de log classiques de Chouette (/srv/log/chouette-ninoxe.log).");
			}
		}
	}
	
	public void importFiles(String[] args) {
		getBeans();
		parseArguments(args);
		System.out.println("Chouette-ninoxe. Version : "+version.getVersion());
		if ((files == null) || (files.size() == 0)) {
			System.out.println("Pas de fichier en entree.");
			printUsage();
			System.exit(9);
		}
		String result = "";
		for (String file : files) {
			System.out.println("Fichier : \""+file+"\"");
			logger.debug("Fichier : \""+file+"\"");
			if (format.equals(FORMAT_XML)) {
				try {
					ChouettePTNetworkType xmlTest = lecteurFichierXML.lire(file, verify);
					if (importer) {
						ILectureEchange lectureEchange = lecteurEchangeXML.lire(xmlTest);
						importateur.importer(false, lectureEchange);
					}
				}
				catch(fr.certu.chouette.service.validation.commun.ValidationException e) {
					if (importer) {
						String str = logValidationException(e, file);
						if (str.length() > 0)
							assert false: "Exception non attendue : \""+str+"\".";
					}
					else {
						String str = logValidationException1(e, file);
						if (str.length() > 0)
							if (valFile != null)
								result = result + str;
							else
								logger.error(str);
					}
				}
				catch(Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			else if (format.equals(FORMAT_CSV)) {
				System.out.println("L'import de fichiers et repertoires n'est pas encore implemente.");
			}
		}
		if (result.length() > 0)
			writeIntoFile(result, valFile);
	}
	
	private String logValidationException(fr.certu.chouette.service.validation.commun.ValidationException e, String fileName) {
		List<TypeInvalidite> categories = e.getCategories();
		StringBuffer str = new StringBuffer();
		StringBuffer str2 = new StringBuffer();
		str2.append("Validation de \""+fileName+"\"\n");
		if (categories != null)
			for (int i = 0; i < categories.size(); i++) {
				TypeInvalidite typeInvalidite = categories.get(i);
				// PERMISSIF SAUF POUR LES NULLSTREETNAME_ADDRESS
				//if (!typeInvalidite.equals(TypeInvalidite.NULLSTREETNAME_ADDRESS) && !typeInvalidite.equals(TypeInvalidite.NULLSTOPPOINTLIST_JOURNEYPATTERN)) {
				{
					str.append(typeInvalidite.toString());
					Set<String> messages = e.getTridentIds(typeInvalidite);
					if (messages != null) {
						str.append(" : ");
						String[] _messages = messages.toArray(new String[0]);
						for (int j = 0; j < _messages.length; j++)
							str.append("\n\t"+_messages[j]);
					}
					str.append("\n");
				}
				str2.append(typeInvalidite.toString());
				Set<String> messages = e.getTridentIds(typeInvalidite);
				if (messages != null) {
					str2.append(" : ");
					String[] _messages = messages.toArray(new String[0]);
					for (int j = 0; j < _messages.length; j++)
						str2.append("\n\t"+_messages[j]);
				}
				str2.append("\n");
			}
		if (str2.length() > 0)
			logger.error(str2);
		return str.toString();
	}
	
	private String logValidationException1(fr.certu.chouette.service.validation.commun.ValidationException e, String fileName) {
		List<TypeInvalidite> categories = e.getCategories();
		StringBuffer str = new StringBuffer();
		if (categories != null)
			for (int i = 0; i < categories.size(); i++) {
				TypeInvalidite typeInvalidite = categories.get(i);
				ResourceBundle myResources = ResourceBundle.getBundle("validationXML");
				String typeInvaliditeSt = typeInvalidite.toString();
				String sheet = myResources.getString(typeInvaliditeSt);
				String sheetMsg = myResources.getString(typeInvaliditeSt+"_MSG");
				Set<String> messages = e.getTridentIds(typeInvalidite);
				if (messages != null) {
					String[] _messages = messages.toArray(new String[0]);
					for (int j = 0; j < _messages.length; j++) {
						str.append(fileName);
						str.append(";");
						str.append(typeInvalidite.toString());
						str.append(";");
						str.append(sheet);
						str.append(";");
						str.append(_messages[j]);
						str.append("\n");
					}
				}
			}
		return str.toString();
	}
	
	private Set<String> getFiles(String repertoire, String format) {
		Set<String> fichiers = new HashSet<String>();
		File rep = new File(repertoire);
		if (!repertoire.endsWith(File.separator))
			repertoire = repertoire + File.separator;
		String[] fics = rep.list();
		for (int i = 0; i < fics.length; i++) {
			String nom = fics[i];
			if (nom.endsWith(format))
				fichiers.add(repertoire+nom);
		}
		return fichiers;
	}
	
	private void writeIntoFile(String str, File file) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			writer.write(str);
			writer.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				writer.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public void printUsage() {
		System.out.println("USAGE:\n  java fr.certu.chouette.shell.ImportXMLMain {<file name>}* [options [optionarg]]");
		System.out.println("    options :");
		System.out.println("      -i : n'importer pas les donnees dans la base chouette_plus dans PostgreSQL.");
		System.out.println("           Seulement valider.");
		System.out.println("      -d,--directory arg : repertoire des fichiers a�importer");
		System.out.println("      -C,--csv : importe les fichiers au format CSV");
		System.out.println("      -X,--xml : importe les fichiers au format XML");
		System.out.println("      -verify={true|false} : verifier la conformit� des fichiers XML au schema Chouette.xsd");
		System.out.println("                             par d�faut \"true\".");
		System.out.println("      -logFile <fileName> : inclure les log dans le fichier <fileName>");
		System.out.println("      -valFile <fileName> : inclure les messages de Validation dans le fichier <fileName>");
		System.out.println("      -logLevel {DEBUG | INFO | WARN | ERROR | FATAL} : indique le niveau de log.");
		System.out.println("");
		System.out.println("      -h,--help : affiche le menu d'aide");
	}
}
