package com.vortexwolf.dvach.services.domain;

import org.apache.http.Header;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.message.BasicHeader;

import android.net.Uri;

import com.vortexwolf.dvach.common.utils.StringUtils;
import com.vortexwolf.dvach.interfaces.IHtmlCaptchaChecker;
import com.vortexwolf.dvach.interfaces.IHttpStringReader;
import com.vortexwolf.dvach.services.presentation.DvachUriBuilder;
import com.vortexwolf.dvach.settings.ApplicationSettings;

public class HtmlCaptchaChecker implements IHtmlCaptchaChecker {
    private final IHttpStringReader mHttpStringReader;
    private final DvachUriBuilder mDvachUriBuilder;
    private final ApplicationSettings mApplicationSettings;

    public HtmlCaptchaChecker(IHttpStringReader httpStringReader, DvachUriBuilder dvachUriBuilder, ApplicationSettings settings) {
        this.mHttpStringReader = httpStringReader;
        this.mDvachUriBuilder = dvachUriBuilder;
        this.mApplicationSettings = settings;
    }

    public CaptchaResult canSkipCaptcha(Uri refererUri){
        return this.canSkipCaptcha(refererUri, true);
    }
    
    @Override
    public CaptchaResult canSkipCaptcha(Uri refererUri, boolean usePasscode) {
        String checkUri = "makaba/captcha.fcgi";
        if (usePasscode && !StringUtils.isEmpty(this.mApplicationSettings.getPasscode())) {
            checkUri += "?usercode=" + this.mApplicationSettings.getPasscode();
        }
        
        Uri uri = this.mDvachUriBuilder.create2chUri(checkUri);

        // Add referer, because it always returns the incorrect value CHECK if not to set it
        Header[] extraHeaders = new Header[] { new BasicHeader("Referer", refererUri.toString()) };
        
        CaptchaResult result;
        try {
            String captchaBlock = this.mHttpStringReader.fromUri(uri.toString(), extraHeaders);
            result = this.checkHtmlBlock(captchaBlock);
            
            if (result.checkAgain && usePasscode) {
                result = this.canSkipCaptcha(refererUri, false);
            }
        } catch (Exception e) {
            result = this.createEmptyResult();
        }
        
        return result;
    }

    public CaptchaResult checkHtmlBlock(String captchaBlock) {
        if(captchaBlock == null){
            return this.createEmptyResult();
        }
        
        CaptchaResult result = new CaptchaResult();
        if (captchaBlock.equals("OK")) {
            result.canSkip = true;
        } else if (captchaBlock.equals("VIP")) {
            result.canSkip = true;
            result.passCode = true;
        } else if (captchaBlock.startsWith("CHECK")) {
            result.canSkip = false;
            result.captchaKey = captchaBlock.substring(captchaBlock.indexOf('\n') + 1);
        } else if (captchaBlock.equals("VIPFAIL")) {
            result.checkAgain = true;
        }

        return result;
    }
    
    private CaptchaResult createEmptyResult(){
        return new CaptchaResult();
    }

    public class CaptchaResult {
        public boolean canSkip;
        public boolean passCode;
        public boolean checkAgain;
        public String captchaKey;
    }
}
