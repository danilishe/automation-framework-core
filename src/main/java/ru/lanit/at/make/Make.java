package ru.lanit.at.make;


import com.fasterxml.jackson.databind.util.Named;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import ru.lanit.at.driver.DriverManager;

import java.util.Arrays;
import java.util.List;

public class Make {

    public static final String SLOW_INPUT = "slow input";
    public static final String LOSE_FOCUS = "lose focus";
    public static final String NO_CLEAR_BEFORE = "no clear before";
    private static final double SEND_KEY_DELAY = .01;
    private Wait wait;

    private DriverManager driverManager;

    private JSExecutor jsExecutor;

    private Logger log = LogManager.getLogger(Make.class);

    public void setWait(Wait wait) {
        this.wait = wait;
    }

    public void setJsExecutor(JSExecutor jsExecutor) {
        this.jsExecutor = jsExecutor;
    }

    public void setDriverManager(DriverManager driverManager) {
        this.driverManager = driverManager;
    }

    /**
     * Scrolls to and makes click on provided {@link WebElement}. After click waits for page and JS loading complete.
     *
     * @param webElement that should be clicked.
     * @deprecated Use {@link AtlasWebElement#click()}
     */
    @Deprecated
    public void clickTo(WebElement webElement) {
        logAction(webElement, "Click on {}");
        wait.untilElementVisible(webElement);
        Boolean iFrameFlag = (Boolean) jsExecutor.executeScript("return(window == top)");
        if (iFrameFlag) {
            scrollIntoView(webElement);
        } else {
            jsExecutor.executeScript("arguments[0].scrollIntoView(true);window.scrollBy(0, -400);", webElement);
        }

        webElement.click();
        wait.untilPageLoaded();
        wait.untilJSComplete();
    }

    public void submit(AtlasWebElement button) {
        logAction(button, "Submit {}");
        button.submit();
    }

    /**
     * Cleans and sends keys to provided {@link WebElement}
     *
     * @param input   The element of page with any kind of input.
     * @param message The message that should be send to an element.
     * @param args    Possible args: {@link Make#NO_CLEAR_BEFORE}, {@link Make#SLOW_INPUT}, {@link Make#LOSE_FOCUS}
     */
    public void sendKeys(AtlasWebElement input, String message, String... args) {
        List<String> params = Arrays.asList(args);
        if (params.isEmpty()) logAction(input, "Sending keys '" + message + "' to {}");
        else logAction(input, "Sending keys '" + message + "' to {} with args {}", args);

        input.click();

        if (!params.contains(NO_CLEAR_BEFORE)) input.clear();

        if (params.contains(SLOW_INPUT)) sendKeysSpelling(input, message);
        else input.sendKeys(message);

        if (params.contains(LOSE_FOCUS)) loseFocus(input);
    }

    private void sendKeysSpelling(WebElement webElement, String message) {
        for (char c : message.toCharArray()) {
            wait.sec(SEND_KEY_DELAY);
            webElement.sendKeys(String.valueOf(c));
        }
    }

    /**
     * @deprecated Use {@link AtlasWebElement#click()}
     */
    @Deprecated
    private void clickToInput(WebElement webElement) {
        try {
            if (webElement.isDisplayed()) clickTo(webElement);
        } catch (WebDriverException ignore) {
            jsClickOn(webElement);
        }
    }

    /**
     * Calls JavasCript method click() on given {@link WebElement}
     *
     * @param webElement
     */
    public void jsClickOn(WebElement webElement) {
        logAction(webElement, "Calling JavaScript click on {}");
        scrollIntoView(webElement);
        jsExecutor.executeScript("arguments[0].click();", webElement);
    }

    /**
     * Extracts xpath from {@link WebElement#toString()}
     * @return XPath of {@link WebElement}
     */
    public String getElementXPath(WebElement webElement){
        String elementToString = webElement.toString();
        String[] xpathParts = elementToString.substring(1, elementToString.length() - 1).split("->");
        StringBuilder xpath = new StringBuilder();

        for (int i = 1; i < xpathParts.length; i++) {
            xpath.append(fixXPath(xpathParts[i].replaceAll("xpath: ", "").trim()));
        }

        return xpath.toString();
    }

    private String fixXPath(String xpath){
        int counter = 0;
        for (int i = 0; i < xpath.length(); i++) {
            char charAtI = xpath.charAt(i);
            if(charAtI == '[') counter--;
            else if(charAtI == ']') counter++;
        }
        if(xpath.startsWith(".")) xpath = "/" + xpath;
        return xpath.substring(0, xpath.length() - counter);
    }

    private void logAction(WebElement webElement, String message, String... args) {
        int stringCutLength = 80;

        String name = null;
        if (webElement instanceof Named) {
            name = ((Named) webElement).getName();
        }
        String elementText = null;
        try {
            elementText = webElement.getText();
        } catch (WebDriverException ignore) {
        }

        if (elementText != null && !elementText.isEmpty()) {
            if (elementText.length() > stringCutLength)
                elementText = elementText.substring(0, stringCutLength) + "...";
            message += " (" + elementText + ")";
            if (name == null || name.isEmpty()) name = elementText;
        }

        log.info(message, name, args);
    }

    /**
     * Emulates mouse focus on element.
     *
     * @param webElement pure WebElement.
     */
    public void focusOnElement(WebElement webElement) {
        logAction(webElement, "Focus on '{}'");
        try {
//            scrollIntoView(webElement);
            new Actions(getDriver())
                    .moveToElement(webElement)
                    .perform();
        } catch (Exception ignore) {
        }
    }

    /**
     * Moves mouse away from element.
     *
     * @param webElement element that should be not in focus.
     */
    public void defocus(WebElement webElement) {
        logAction(webElement, "Losing focus from {} by moving mouse away.");
        new Actions(getDriver()).moveByOffset(webElement.getSize().width / 2 + 5, webElement.getSize().height / 2 + 5).perform();
    }

    /**
     * Moves mouse away and clicks to completely lose focus on element.
     *
     * @param webElement element that should be not in focus.
     */
    public void loseFocus(WebElement webElement) {
        logAction(webElement, "Losing focus from element {} by clicking");
//        jsExecutor.executeScript("arguments[0].blur();", webElement);
        new Actions(getDriver()).moveToElement(webElement, -webElement.getRect().x - 3, 0).click().build().perform();
    }

    /**
     * @deprecated Use {@link WrapsDriver#getWrappedDriver()}
     */
    @Deprecated
    public WebDriver getDriver() {
        return driverManager.getDriver();
    }

    public void scrollIntoView(WebElement webElement) {
        jsExecutor.executeScript(
                "arguments[0].scrollIntoView(true);window.scrollBy(0, -400);",
                webElement);
    }

    public boolean checkElementExist(WebElement webElement) {
        try {
            return webElement.isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }
}
