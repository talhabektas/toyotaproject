/**
 * USDTRY kuru hesaplama fonksiyonu
 * @param {Object} dependencies - Bağımlı kurları içeren nesne
 * @return {Object} bid ve ask değerlerini içeren hesaplanmış kur
 */
function calculate(dependencies) {

    const pf1UsdTry = dependencies["PF1_USDTRY"];
    const pf2UsdTry = dependencies["PF2_USDTRY"];

    const bid = (pf1UsdTry.bid + pf2UsdTry.bid) / 2;
    const ask = (pf1UsdTry.ask + pf2UsdTry.ask) / 2;

    return { bid, ask };
}