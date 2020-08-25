<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        Please provider your phone number
    <#elseif section = "form">
        <form id="kc-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <select id="regionPrefix"
                        required
                        name="regionPrefix"
                        value="${(regionPrefix)!""}"
                        class="custom-select custom-select-lg">
                    <#list countries as l>
                        <#if (((regionPrefix)!"") == l.code)>
                            <option selected value="${l.code}">${l.label}</option>
                        <#else>
                            <option value="${l.code}">${l.label}</option>
                        </#if>
                    </#list>
                </select>
            </div>

            <div class="input-group flex-nowrap mb-3">
                <div class="input-group-prepend mw-70">
                    <span class="input-group-text w-100 bg-white" id="region-addon-wrapping"></span>
                </div>
                <input type="number"
                       required
                       id="phone"
                       name="phone"
                       autofocus
                       class="form-control form-control-lg rounded-right"
                       value="${(phoneNumber)!""}"/>
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
