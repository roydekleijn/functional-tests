package nl.vpro.poms.selenium.poms.admin;

import org.junit.Test;

import nl.vpro.poms.selenium.pages.AddNewObjectOverlayPage;
import nl.vpro.poms.selenium.pages.Login;
import nl.vpro.poms.selenium.pages.OmroepenOverlayPage;
import nl.vpro.poms.selenium.pages.Search;
import nl.vpro.poms.selenium.poms.AbstractTest;
import nl.vpro.poms.selenium.util.DateFactory;
import nl.vpro.poms.selenium.util.WebDriverFactory;


public class AdminTest extends AbstractTest {

	public AdminTest(WebDriverFactory.Browser browser) {
		super(browser);
	}

	@Test
	public void testAdmin() {
		loginSpeciaalAdminGebruiker();
		
		Search search = new Search(driver);
		search.clickNew();
		AddNewObjectOverlayPage addOverlay = new AddNewObjectOverlayPage(driver);
		String title = "Test " + DateFactory.getNow();
		addOverlay.enterTitle(title);
		addOverlay.chooseMediaType("Clip");
		addOverlay.chooseAvType("Video");
		addOverlay.chooseGenre("Jeugd");
		addOverlay.clickMaakAan();
		
		addOverlay.clickHerlaad();
		
		search.enterQuery(title);
		search.clickZoeken();
//		logout();
	}
	
	@Test
	public void testAddAndRemoveOmroep() {
		loginSpeciaalAdminGebruiker();
		Search search = new Search(driver);
		search.clickAdminItem("omroepen");
		waitForAngularRequestsToFinish();

		OmroepenOverlayPage overlay = new OmroepenOverlayPage(driver);
		overlay.addOmroep("Test");
		overlay.close();
		
		search.clickAdminItem("omroepen");
		
		
//		logout();
	}
	
	private void loginSpeciaalAdminGebruiker() {
		Login login = new Login(driver);
		login.gotoPage();
		String user = CONFIG.getProperties().get("AdminGebruiker.LOGIN");
		String password = CONFIG.getProperties().get("AdminGebruiker.PASSWORD");
		login.login(user, password);
	}

}
