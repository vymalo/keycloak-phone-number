<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('missing_phone_number_or_region', 'phoneNumber'); section>
    <#if section = "header">
        Please provider your phone number
    <#elseif section = "form">
        <form id="kc-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <label for="regionPrefix"
                       class="${properties.kcLabelClass!}">${msg("Country")}</label>
                <select id="regionPrefix"
                        required
                        name="regionPrefix"
                        value="${(regionPrefix)!""}"
                        class="${properties.kcInputClass!}">
                    <#list countries as l>
                        <#if (((regionPrefix)!"") == l.code)>
                            <option selected value="${l.code}">${l.label}</option>
                        <#else>
                            <option value="${l.code}">${l.label}</option>
                        </#if>
                    </#list>
                </select>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <label for="phone"
                       class="${properties.kcLabelClass!}">${msg("phoneNumber")}</label>
                <input type="text"
                       required
                       id="phone"
                       name="phone"
                       autofocus
                       class="${properties.kcInputClass!}"
                       value="${(phoneNumber)!""}"/>

                <#if messagesPerField.existsError('phoneNumber')>
                    <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                        ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                    </span>
                </#if>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doContinue")}"/>
                </div>

            </div>
            <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                <div id="kc-registration">
                    <span>${msg("noAccount")} <a tabindex="6"
                                                 href="${url.registrationUrl}">${msg("doRegister")}</a></span>
                </div>
            </#if>
        </form>

        <#if realm.password && social.providers??>
            <div id="kc-social-providers">
                <#list social.providers as p>
                    <a href="${p.loginUrl}" id="zocial-${p.alias}" class="social ${p.providerId}">
                        <span>${p.displayName}</span>
                    </a>
                </#list>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
