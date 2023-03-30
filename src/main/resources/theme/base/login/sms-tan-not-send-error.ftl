<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        Tan Error
    <#elseif section = "form">
        <form id="kc-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                           name="cancel" id="kc-decline" type="submit" value="${msg("doCancel")}"/>
                </div>

            </div>
        </form>
    </#if>
</@layout.registrationLayout>
