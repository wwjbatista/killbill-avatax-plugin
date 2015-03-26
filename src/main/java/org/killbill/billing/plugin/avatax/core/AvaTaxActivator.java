/*
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.avatax.core;

import java.util.Hashtable;

import org.killbill.billing.invoice.plugin.api.InvoicePluginApi;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.plugin.avatax.api.AvaTaxInvoicePluginApi;
import org.killbill.billing.plugin.avatax.api.TaxRatesInvoicePluginApi;
import org.killbill.billing.plugin.avatax.client.AvaTaxClient;
import org.killbill.billing.plugin.avatax.client.TaxRatesClient;
import org.killbill.billing.plugin.avatax.dao.AvaTaxDao;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.osgi.framework.BundleContext;

import com.google.common.base.Strings;

public class AvaTaxActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-avatax";

    public static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.avatax.";
    public static final String TAX_RATES_API_PROPERTY_PREFIX = "org.killbill.billing.plugin.avatax.taxratesapi.";

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final AvaTaxDao dao = new AvaTaxDao(dataSource.getDataSource());
        final Clock clock = new DefaultClock();

        final String proxyPortString = configProperties.getString(PROPERTY_PREFIX + "proxyPort");
        final String strictSSLString = configProperties.getString(PROPERTY_PREFIX + "strictSSL");
        final String proxyHost = configProperties.getString(PROPERTY_PREFIX + "proxyHost");
        final Integer proxyPort = Strings.isNullOrEmpty(proxyPortString) ? null : Integer.valueOf(proxyPortString);
        final boolean strictSSL = Strings.isNullOrEmpty(strictSSLString) ? true : Boolean.valueOf(strictSSLString);

        // Avalara AvaTax API
        final String avaTaxUrl = configProperties.getString(PROPERTY_PREFIX + "url");
        final String avaTaxAccountNumber = configProperties.getString(PROPERTY_PREFIX + "accountNumber");
        final String avaTaxLicenseKey = configProperties.getString(PROPERTY_PREFIX + "licenseKey");

        // Avalara Tax Rates API
        final String taxRatesApiUrl = configProperties.getString(TAX_RATES_API_PROPERTY_PREFIX + "url");
        final String taxRatesApiApiKey = configProperties.getString(TAX_RATES_API_PROPERTY_PREFIX + "apiKey");

        final InvoicePluginApi invoicePluginApi;
        if (avaTaxUrl != null && avaTaxAccountNumber != null && avaTaxLicenseKey != null) {
            final AvaTaxClient avataxClient = new AvaTaxClient(avaTaxUrl,
                                                               avaTaxAccountNumber,
                                                               avaTaxLicenseKey,
                                                               proxyHost,
                                                               proxyPort,
                                                               strictSSL);
            invoicePluginApi = new AvaTaxInvoicePluginApi(avataxClient,
                                                          dao,
                                                          configProperties.getString(PROPERTY_PREFIX + "companyCode"),
                                                          killbillAPI,
                                                          configProperties,
                                                          logService,
                                                          clock);
        } else if (taxRatesApiUrl != null && taxRatesApiApiKey != null) {
            final TaxRatesClient taxRatesClient = new TaxRatesClient(taxRatesApiUrl,
                                                                     taxRatesApiApiKey,
                                                                     proxyHost,
                                                                     proxyPort,
                                                                     strictSSL);
            invoicePluginApi = new TaxRatesInvoicePluginApi(taxRatesClient,
                                                            dao,
                                                            killbillAPI,
                                                            configProperties,
                                                            logService,
                                                            clock);
        } else {
            throw new IllegalStateException("AvaTax plugin mis-configured!");
        }
        registerInvoicePluginApi(context, invoicePluginApi);
    }

    @Override
    public OSGIKillbillEventDispatcher.OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return null;
    }

    private void registerInvoicePluginApi(final BundleContext context, final InvoicePluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, InvoicePluginApi.class, api, props);
    }
}
