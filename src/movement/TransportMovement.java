/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;

/**
 * MovementModels used for transportation should implement this interface
 * 
 * @author Frans Ekman
 */
public interface TransportMovement extends SwitchableMovement {

	public void setNextRoute(Coord nodeLocation, Coord nodeDestination);

}
