<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header" || section = "show-username">
        <h1 id="kc-page-title">
            Please enter the Code
        </h1>
        <style>#kc-username {display: none;}</style>
    <#elseif section = "form">
        <form id="kc-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div id="kc-info-message">
                <p class="confirm-field">We sent a code at</p>
                <p class="phoneNumber-field"><b>${phoneNumber}</b></p>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <label for="smsCode"
                       class="${properties.kcLabelClass!}">
                    ${msg("Code")}
                </label>
                <input type="text"
                       placeholder="${msg("Code")}"
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
