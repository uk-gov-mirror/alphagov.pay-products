package uk.gov.pay.products.pact;

import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("products")
@PactBroker(scheme = "https", host = "${PACT_BROKER_HOST:pact-broker-test.cloudapps.digital}", tags = {"${PACT_CONSUMER_TAG}", "test"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"selfservice"})
@IgnoreNoPactsToVerify
public class SelfServiceContractTest extends ContractTest {
}
