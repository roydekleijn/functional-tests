package nl.vpro.poms.selenium.poms.wijzigen;

import nl.vpro.poms.selenium.pages.Search;
import nl.vpro.poms.selenium.poms.AbstractTest;
import nl.vpro.poms.selenium.util.WebDriverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChangeTest extends AbstractTest {

	public ChangeTest(WebDriverFactory.Browser browser) {
		super(browser);
	}

	@Test
	public void testWijzig() {
		login().speciaalVf();
		Search search = new Search(driver);
		search.selectOptionFromMenu("Omroepen", "VPRO");
		search.selectOptionFromMenu("MediaType", "Clip");
//		List<WebElement> tableRows = search.getTableRows();
//		for (WebElement row: tableRows) {
//			System.out.println("###" + row);
//		}
//		WebElement row = tableRows.get(0);
//		Actions actions = new Actions(driver);
//		actions.doubleClick(row);
		search.clickRow(0);
		search.scrollToAfbeeldingen();
		logout();
	}
	
	@Test
	public void testWissen() {
		login().speciaalVf();
		Search search = new Search(driver);
		search.selectOptionFromMenu("Omroepen", "VPRO");
		search.selectOptionFromMenu("MediaType", "Clip");
		search.clickWissen();
		logout();
	}
	
	@Test
	public void testNietBewerken() {
		login().speciaalVf();
		Search search = new Search(driver);
		search.clickWissen();
		search.selectOptionFromMenu("Omroepen", "NPO");
		search.selectOptionFromMenu("Criteria", "Mag schrijven");
		
		logout();
	}


}