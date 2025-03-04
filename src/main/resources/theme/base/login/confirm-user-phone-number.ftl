<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        Confirm your Phone number
    <#elseif section = "form">
        <form id="kc-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div id="kc-info-message">
                <p class="confirm-field">Is this number correct ?</p>
                <p class="phoneNumber-field"><b>${phoneNumber}</b></p>
                <p class="instruction-field">We'll send an sms to this number with a code for next steps</p>
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
