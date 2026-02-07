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
                        <#if (((regionPrefix)!"") == l.code())>
                            <option selected value="${l.code()}">${l.label()}</option>
                        <#else>
                            <option value="${l.code()}">${l.label()}</option>
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

    <#elseif section = "socialProviders" >
        <#if realm.password && social?? && social.providers?has_content>
            <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                <hr/>
                <h2>${msg("identity-provider-login-label")}</h2>

                <ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>">
                    <#list social.providers as p>
                        <li>
                            <a id="social-${p.alias}"
                               class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                               type="button" href="${p.loginUrl}">
                                <#if p.iconClasses?has_content>
                                    <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}"
                                       aria-hidden="true"></i>
                                    <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                                <#else>
                                    <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                                </#if>
                            </a>
                        </li>
                    </#list>
                </ul>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
