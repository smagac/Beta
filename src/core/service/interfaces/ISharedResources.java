package core.service.interfaces;

import github.nhydock.ssm.Service;

public interface ISharedResources extends Service {

    public <T> T getResource(String name, Class<T> cls);
}
