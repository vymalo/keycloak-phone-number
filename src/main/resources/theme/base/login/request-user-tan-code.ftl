<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        Please enter the Code
    <#elseif section = "form">
        <form id="kc-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div id="kc-info-message">
                <input type="number"
                       required
                       id="smsCode"
                       name="code"
                       class="${properties.kcInputClass!}"
                       autofocus
                       value="${(smsCode)!""}"/>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doContinue")}"/>

                    <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                           name="cancel" id="kc-decline" type="submit" value="${msg("doDecline")}"/>
                </div>

            </div>
        </form>
    </#if>
</@layout.registrationLayout>
