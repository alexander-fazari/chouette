package fr.certu.chouette.ihm.horairesdepassage;

import chouette.schema.types.DayTypeType;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;
import fr.certu.chouette.ihm.GeneriqueAction;
import fr.certu.chouette.ihm.outil.pagination.Pagination;
import fr.certu.chouette.ihm.struts.ModelInjectable;
import fr.certu.chouette.modele.ArretItineraire;
import fr.certu.chouette.modele.Course;
import fr.certu.chouette.modele.Horaire;
import fr.certu.chouette.modele.Mission;
import fr.certu.chouette.modele.PositionGeographique;
import fr.certu.chouette.modele.TableauMarche;
import fr.certu.chouette.service.database.ICourseManager;
import fr.certu.chouette.service.database.IHoraireManager;
import fr.certu.chouette.service.database.IItineraireManager;
import fr.certu.chouette.service.database.IMissionManager;
import fr.certu.chouette.service.database.IPositionGeographiqueManager;
import fr.certu.chouette.service.database.impl.modele.EtatMajHoraire;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.components.ActionError;
import org.apache.struts2.interceptor.validation.SkipValidation;

@SuppressWarnings({ "serial", "unchecked" })
public class HorairesDePassageAction extends GeneriqueAction implements ModelInjectable, ModelDriven, Preparable {
	
	private static final Log                          log                         = LogFactory.getLog(HorairesDePassageAction.class);
	private              ICourseManager               courseManager;
	private              IPositionGeographiqueManager positionGeographiqueManager;
	private              IHoraireManager              horaireManager;
	private              IItineraireManager           itineraireManager;
	private              IMissionManager              missionManager;
	private              int                          maxNbCoursesParPage;
	private              int                          maxNbCalendriersParCourse;
	private              List<Course>                 courses;
	private              Pagination                   pagination;
	private              Object                       model;
	
	// MODEL & PREPARE _________________________________________________________________________
	// NE STOCKER DANS LE MODEL
	// QUE LES DONNEES UTILES A
	// L' AFFICHAGE  DE  LA VUE
		
	public void setModel(Object model) {
		this.model = model;
	}
	
	public Object getModel() {
		return model;
	}
	
	public void prepare() throws Exception {
		Long idItineraire = ((ListHorairesDePassageModel)model).getIdItineraire();
		log.debug("idItineraire                             : "+idItineraire.longValue());
		// RECUPERATION DES COURSES
		courses = courseManager.getCoursesFiltrees(idItineraire, ((ListHorairesDePassageModel)model).getIdTableauMarche(), ((ListHorairesDePassageModel)model).getSeuilDateDepartCourse());
		log.debug("courses.size()                           : "+courses.size());
		// GESTION DE LA PAGINATION
		if (pagination.getNumeroPage() == null || pagination.getNumeroPage() < 1)
			pagination.setNumeroPage(1);
		pagination.setNbTotalColonnes(courses.size());
		List <Course> coursesPage = (List <Course>)pagination.getCollectionPageCourante(courses);
		log.debug("coursesPage.size()                       : "+coursesPage.size());
		((ListHorairesDePassageModel)model).setCoursesPage(coursesPage);
		// GESTION DES ARRETS DE L'ITINERAIRE
		List <ArretItineraire> arretsItineraire = itineraireManager.getArretsItineraire(idItineraire);
		log.debug("arretsItineraire.size()                  : "+arretsItineraire.size());
		((ListHorairesDePassageModel)model).setArretsItineraire(arretsItineraire);
		Map <Long, PositionGeographique> arretPhysiqueParIdArret = positionGeographiqueManager.getArretPhysiqueParIdArret(arretsItineraire);
		((ListHorairesDePassageModel)model).setArretPhysiqueParIdArret(arretPhysiqueParIdArret);
		// PREPARATION DE LA LISTE DES TM POUR LE FILTRE
		((ListHorairesDePassageModel)model).setTableauxMarche(itineraireManager.getTableauxMarcheItineraire(idItineraire));
		// PREPARATION DES ELEMENTS NECESSAIRES A L'AFFICHAGE
		// DE L'ENTETE DU TABLEAU
		prepareMapPositionArretParIdArret(arretsItineraire);	
		prepareHoraires(arretsItineraire, coursesPage);
		prepareMapMissionParIdCourse(coursesPage);
		prepareMapsTableauxMarche();
	}

	private void prepareMapPositionArretParIdArret(List <ArretItineraire> arretsItineraire) {
		Map	<Long, Integer> positionArretParIdArret = new Hashtable<Long, Integer>();
		for (ArretItineraire arret : arretsItineraire)
			positionArretParIdArret.put(arret.getId(), arret.getPosition());
		((ListHorairesDePassageModel)model).setPositionArretParIdArret(positionArretParIdArret);
	}
	
	private void prepareHoraires (List <ArretItineraire> arretsItineraire, List <Course> coursesPage) {
		if (coursesPage != null && arretsItineraire.size() > 0) {
			List <Long> idsCourses = new ArrayList<Long>();
			for (Course course : coursesPage)
				idsCourses.add(course.getId());
			Map <Long, List<Horaire>> horairesCourseParIdCourse = getMapHorairesCourseParIdCourse(idsCourses);
			Map	<Long, List<Horaire>> horairesCourseOrdonneesParIdCourse = new HashMap <Long, List<Horaire>>();
			// RECUPERER LES HORAIRES DE COURSE POUR
			// CHAQUE COURSE DE LA PAGE
			List<Horaire> tmpHorairesCourseOrdonnees = new ArrayList<Horaire>();
			for (Long idCourse : idsCourses) {
				List<Horaire> horairesCourseOrdonnees = obtenirHorairesCourseOrdonnees(horairesCourseParIdCourse.get(idCourse), arretsItineraire);
				horairesCourseOrdonneesParIdCourse.put(idCourse, horairesCourseOrdonnees);
				tmpHorairesCourseOrdonnees.addAll(horairesCourseOrdonnees);
			}
			log.debug("horairesCourses.size()               : "+tmpHorairesCourseOrdonnees.size());
			((ListHorairesDePassageModel)model).setHorairesCourses(tmpHorairesCourseOrdonnees);
			((ListHorairesDePassageModel)model).setHorairesParIdCourse(horairesCourseOrdonneesParIdCourse);
			// RECUPERER LES HEURES DE COURSE POUR
			// CHAQUE COURSE DE LA PAGE
			idsCourses = new ArrayList<Long>();
			for (Course course : coursesPage)
				idsCourses.add(course.getId());
			List <Date> heuresCourses = new ArrayList <Date> (idsCourses.size() * arretsItineraire.size());
			for (Long idCourse : idsCourses) {
				List<Horaire> horairesCourseOrdonnees = obtenirHorairesCourseOrdonnees(horairesCourseParIdCourse.get(idCourse), arretsItineraire);
				heuresCourses.addAll(obtenirDatesDepartFromHoraires(horairesCourseOrdonnees));
			}
			((ListHorairesDePassageModel)model).setHeuresCourses(heuresCourses);
		}
	}

	private List<Date> obtenirDatesDepartFromHoraires(List<Horaire> horaires) {
		List<Date> dates = new ArrayList<Date>(horaires.size());
		for (Horaire horaireCourse : horaires)
			dates.add(horaireCourse == null ? null : horaireCourse.getDepartureTime());
		return dates;
	}
	
	private List<Horaire> obtenirHorairesCourseOrdonnees (List<Horaire> horairesCourse, List <ArretItineraire> arretsItineraire) {
		Horaire[] horairesCourseOrdonnees = new Horaire[arretsItineraire.size()];
		if (horairesCourse != null) {
			for (Horaire horaireCourse : horairesCourse) {
				Integer positionHoraire = ((ListHorairesDePassageModel)model).getPositionArretParIdArret().get(horaireCourse.getIdArret());
				if (positionHoraire != null)
					horairesCourseOrdonnees[positionHoraire] = horaireCourse;
				else
					log.error("L'horaire " + horaireCourse.getId() + " à l'arret " + horaireCourse.getIdArret() + " n'a pas de position connue sur l'itinéraire!");
			}
		}
		return Arrays.asList(horairesCourseOrdonnees);
	}
	
	private Map <Long, List<Horaire>> getMapHorairesCourseParIdCourse (Collection <Long> idCourses) {
		List <Horaire> horaires = courseManager.getHorairesCourses(idCourses);
		Map <Long, List<Horaire>> horairesCourseParIdCourse = new Hashtable <Long, List<Horaire>> ();
		for (Horaire horaire : horaires) {
			Long idCourseCourante = horaire.getIdCourse();
			List <Horaire> horairesCourse = horairesCourseParIdCourse.get(idCourseCourante);
			if (horairesCourse == null) {
				horairesCourse = new ArrayList<Horaire>();
				horairesCourseParIdCourse.put(idCourseCourante, horairesCourse);
			}
			horairesCourse.add(horaire);
		}
		return horairesCourseParIdCourse;
	}

	private void prepareMapMissionParIdCourse (List <Course> coursesPage) {
		List <Long> idsMissionAffichee= new ArrayList <Long>();
		for (Course course : coursesPage)
			idsMissionAffichee.add(course.getIdMission());
		List <Mission> missions = missionManager.getMissions(idsMissionAffichee);
		Map <Long, Mission> missionParIdCourse = new Hashtable <Long, Mission>();
		for (Mission mission : missions)
			missionParIdCourse.put(mission.getId(), mission);
		((ListHorairesDePassageModel)model).setMissionParIdCourse(missionParIdCourse);
	}
	
	private void prepareMapsTableauxMarche() {
		Map <Long, List<TableauMarche>> tableauxMarcheParIdCourse = new HashMap<Long, List<TableauMarche>>();
		Map <Long, Integer> referenceTableauMarcheParIdTableauMarche = new HashMap<Long, Integer>();
		int index = 1;
		List <TableauMarche> tableauxMarcheCourse;
		for (Course course : courses) {
			tableauxMarcheCourse = courseManager.getTableauxMarcheCourse(course.getId());
			tableauxMarcheParIdCourse.put(course.getId(), tableauxMarcheCourse);
			for (TableauMarche tableauMarche : tableauxMarcheCourse)
				if (referenceTableauMarcheParIdTableauMarche.get(tableauMarche.getId()) == null)
					referenceTableauMarcheParIdTableauMarche.put(tableauMarche.getId(), index++);
		}
		((ListHorairesDePassageModel)model).setTableauxMarcheParIdCourse(tableauxMarcheParIdCourse);
		((ListHorairesDePassageModel)model).setReferenceTableauMarcheParIdTableauMarche(referenceTableauMarcheParIdTableauMarche);
	}
	
	// LIST ____________________________________________________________________________________
	
	@SkipValidation
	public String list() {
		return SUCCESS;
	}
	
	// CRUD ____________________________________________________________________________________
	
	public String editerHorairesCourses() {
		List <Date> heuresCourses = ((ListHorairesDePassageModel)model).getHeuresCourses();
		log.debug("heuresCourses.size()                     : "+heuresCourses.size());
		List <ArretItineraire> arretsItineraire = ((ListHorairesDePassageModel)model).getArretsItineraire();
		log.debug("arretsItineraire.size()                  : "+arretsItineraire.size());
		List <Integer> idsHorairesInvalides = horaireManager.filtreHorairesInvalides (heuresCourses, arretsItineraire.size());
		((ListHorairesDePassageModel)model).setIdsHorairesInvalides(idsHorairesInvalides);
		if (idsHorairesInvalides != null && !idsHorairesInvalides.isEmpty()) {	
			addActionError(getText("error.horairesInvalides"));
			return INPUT;
		}
		int indexPremiereDonneeDansCollectionPaginee = pagination.getIndexPremiereDonneePageCouranteDansCollectionPaginee(arretsItineraire.size()); 
		log.debug("indexPremiereDonneeDansCollectionPaginee : "+indexPremiereDonneeDansCollectionPaginee);
		log.debug("horairesCourses.size()                   : "+((ListHorairesDePassageModel)model).getHorairesCourses().size());
		Collection<EtatMajHoraire> majHoraires = new ArrayList<EtatMajHoraire>();
		for (int i = 0; i < ((ListHorairesDePassageModel)model).getHorairesCourses().size(); i++) {
			Date heureCourse = ((ListHorairesDePassageModel)model).getHeuresCourses().get(i);
			Horaire horaireCourse = ((ListHorairesDePassageModel)model).getHorairesCourses().get(i);
			if (horaireCourse == null)
				log.debug("horaireCourse.getIdCourse()              : NULL");
			else
				log.debug("idCourse : stopPointId : departureTime   : "+horaireCourse.getIdCourse()+" : "+horaireCourse.getStopPointId()+" : "+horaireCourse.getDepartureTime().toString());
			if (heureCourse != null && horaireCourse == null) {
				EtatMajHoraire etatMajHoraire = EtatMajHoraire.getCreation (
						getIdArretParIndice(indexPremiereDonneeDansCollectionPaginee, arretsItineraire), 
						getIdCourseParIndice(indexPremiereDonneeDansCollectionPaginee, arretsItineraire), 
						heureCourse);
				majHoraires.add (etatMajHoraire);
			}
			else if (heureCourse == null && horaireCourse != null)
				majHoraires.add(EtatMajHoraire.getSuppression(horaireCourse));
			else if (areBothDefinedAndDifferent(heureCourse, horaireCourse)) {
				horaireCourse.setDepartureTime(heureCourse);
				majHoraires.add(EtatMajHoraire.getModification(horaireCourse));
			}
			indexPremiereDonneeDansCollectionPaginee ++;
		}
		horaireManager.modifier(majHoraires);
		return SUCCESS;
	}
	
	public String editerHorairesCoursesConfirmation() {
		List <ArretItineraire> arretsItineraire = ((ListHorairesDePassageModel)model).getArretsItineraire();
		int indexPremiereDonneePagination = pagination.getIndexPremiereDonneePageCouranteDansCollectionPaginee(arretsItineraire.size()); 
		Collection<EtatMajHoraire> majHoraires = new ArrayList<EtatMajHoraire>();
		for (int i = 0; i < ((ListHorairesDePassageModel)model).getHorairesCourses().size(); i++) {
			Date heureDepart = ((ListHorairesDePassageModel)model).getHeuresCourses().get(i);
			Horaire horaire = ((ListHorairesDePassageModel)model).getHorairesCourses().get(i);
			if (heureDepart != null && horaire == null)
				majHoraires.add(EtatMajHoraire.getCreation (
						getIdArretParIndice(indexPremiereDonneePagination, arretsItineraire), 
						getIdCourseParIndice(indexPremiereDonneePagination, arretsItineraire), 
						heureDepart));
			else if (heureDepart == null && horaire != null)
				majHoraires.add(EtatMajHoraire.getSuppression(horaire));
			else if (areBothDefinedAndDifferent(heureDepart, horaire)) {
				horaire.setDepartureTime(heureDepart);
				majHoraires.add(EtatMajHoraire.getModification(horaire));
			}
			indexPremiereDonneePagination ++;
		}
		horaireManager.modifier(majHoraires);
		return SUCCESS;
	}
	
	public String ajoutCourseAvecDecalageTemps() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(((ListHorairesDePassageModel)model).getTempsDecalage());
		long tempsDecalageMillis = cal.get(Calendar.HOUR_OF_DAY) * 3600000 + cal.get(Calendar.MINUTE) * 60000;
		List <ArretItineraire> arretsItineraire = ((ListHorairesDePassageModel)model).getArretsItineraire();
		Long idCourseADecaler = ((ListHorairesDePassageModel)model).getIdCourseADecaler();
		Integer nbreCourseDecalage = ((ListHorairesDePassageModel)model).getNbreCourseDecalage();
		if (idCourseADecaler != null && nbreCourseDecalage >= 1) {
			// Récupération des horaires de la course qu'il faut décaler d'un
			// certain temps
			List<Horaire> horairesADecaler = ((ListHorairesDePassageModel)model).getHorairesParIdCourse().get(idCourseADecaler);
			//log.debug("horairesADecaler : " + horairesADecaler);
			List<Horaire> horairesADecalerResultat = new ArrayList<Horaire>();
			//création de la liste des ids des tableaux de marche de la course de référence
			List<Long> tableauxMarcheIds = new ArrayList<Long>();
			List<TableauMarche> tms = ((ListHorairesDePassageModel)model).getTableauxMarcheParIdCourse().get(idCourseADecaler);
			for(TableauMarche tm : tms)
				tableauxMarcheIds.add(tm.getId());
			for (int i = 0; i < nbreCourseDecalage; i++) {
				// Création d'une course
				Course course = new Course();
				course.setIdItineraire(((ListHorairesDePassageModel)model).getIdItineraire());
				course.setPublishedJourneyName("Pas de nom");
				courseManager.creer(course);
				// Copie des tableaux de marche de la course de référence dans la nouvelle
				courseManager.associerCourseTableauxMarche(course.getId(), tableauxMarcheIds);
				// Ajout du temps de décalage à toutes les dates
				int compteurHoraire = 0;
				Collection<EtatMajHoraire> majHoraires = new ArrayList<EtatMajHoraire>();
				for (Horaire horaire : horairesADecaler) {
					if (horaire != null) {
						Date heureDepartOrigine = horaire.getDepartureTime();
						Date heureDepartResultat = new Date(heureDepartOrigine.getTime() + tempsDecalageMillis);
						//	Mise à jour de la liste d'horaire résultat
						Horaire horaireResultat = new Horaire();
						horaireResultat.setIdArret(horaire.getIdArret());
						horaireResultat.setIdCourse(horaire.getIdCourse());
						horaireResultat.setDepartureTime(heureDepartResultat);
						horairesADecalerResultat.add(horaireResultat);
						Long idArretItineraire = getIdArretParIndice(compteurHoraire, arretsItineraire);
						majHoraires.add(EtatMajHoraire.getCreation(idArretItineraire, course.getId(), heureDepartResultat));
					}
					else
						horairesADecalerResultat.add(null);
					compteurHoraire++;
				}
				horaireManager.modifier(majHoraires);
				horairesADecaler = horairesADecalerResultat;
				horairesADecalerResultat = new ArrayList<Horaire>();
			}
		}
		return SUCCESS;
	}
	
	// MANAGERS ________________________________________________________________________________
	
	public void setCourseManager(ICourseManager courseManager) {
		this.courseManager = courseManager;
	}
	
	public void setHoraireManager(IHoraireManager horaireManager) {
		this.horaireManager = horaireManager;
	}
	
	public void setItineraireManager(IItineraireManager itineraireManager) {
		this.itineraireManager = itineraireManager;
	}
	
	public void setMissionManager(IMissionManager missionManager) {
		this.missionManager = missionManager;
	}
	
	public void setPositionGeographiqueManager(IPositionGeographiqueManager positionGeographiqueManager) {
		this.positionGeographiqueManager = positionGeographiqueManager;
	}
	
	// MISC ____________________________________________________________________________________
	
	public String cancel() {
		((ListHorairesDePassageModel)model).setIdTableauMarche(null);
		((ListHorairesDePassageModel)model).setSeuilDateDepartCourse(null);
		addActionMessage(getText("horairesDePassage.cancel.ok"));
		return SUCCESS;
	}
	
	@Override
	public String input() throws Exception {
		return SUCCESS;
	}
	
	private boolean areBothDefinedAndDifferent(Date heureSaisie, Horaire horaireBase) {
		return heureSaisie != null && horaireBase != null && horaireBase.getDepartureTime() != null && (horaireBase.getDepartureTime().compareTo(heureSaisie) != 0);
	}
	
	private Long getIdArretParIndice (int indice, List <ArretItineraire> arretsItineraire) {
		int indiceArret = indice % arretsItineraire.size();
		return arretsItineraire.get(indiceArret).getId();
	}
	
	private Long getIdCourseParIndice (int indice, List <ArretItineraire> arretsItineraire) {
		int indiceCourse = indice / arretsItineraire.size();
		return courses.get(indiceCourse).getId();
	}

	public boolean isDayTypeDansDayTypes(DayTypeType dayType, Set<DayTypeType> dayTypes) {
		if (dayTypes.contains(dayType))
			return true;
		return false;
	}
	
	public Boolean isErreurAjoutCourseAvecDecalageTemps() {
		if (getFieldErrors().containsKey("tempsDecalage") || getFieldErrors().containsKey("nbreCourseDecalage"))
			return true;
		return false;
	}
	
	public Boolean isErreurHorairesInvalides() {
		Collection <ActionError> actionErrors = getActionErrors();
		if (actionErrors.contains(getText("error.horairesInvalides")))
			return true;
		return false;
	}
	
	public int getPage() {
		return pagination.getNumeroPage();
	}

	public void setPage(int page) {
		pagination.setNumeroPage(page);
	}
	
	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}

	public int getMaxNbCoursesParPage() {
		return maxNbCoursesParPage;
	}

	public void setMaxNbCoursesParPage(int maxNbCoursesParPage) {
		this.maxNbCoursesParPage = maxNbCoursesParPage;
	}

	public int getMaxNbCalendriersParCourse() {
		return maxNbCalendriersParCourse;
	}

	public void setMaxNbCalendriersParCourse(int maxNbCalendriersParCourse) {
		this.maxNbCalendriersParCourse = maxNbCalendriersParCourse;
	}
}
