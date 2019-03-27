import realtimeSaga from 'ra-realtime';

const observeRequest = dataProvider => (type, resource, params) => {
    // Filtering so that only LISTED RESOURCES are updated in real time
    if (type === 'GET_LIST' && (resource === 'cubes' || resource === 'commands')) {
        return {
            subscribe(observer) {
                let intervalId = setInterval(() => {
                    dataProvider(type, resource, params)
                        .then(results => observer.next(results)) // New data received, notify the observer
                        .catch(error => observer.error(error)); // Ouch, an error occured, notify the observer
                }, 5000);

                const subscription = {
                    unsubscribe() {
                        if (intervalId) {
                            // Clean up after ourselves
                            clearInterval(intervalId);
                            intervalId = undefined;
                            // Notify the saga that we cleaned up everything
                            observer.complete();
                        }
                    }
                };

                return subscription;
            },
        };
    }
};

export default dataProvider => realtimeSaga(observeRequest(dataProvider));
