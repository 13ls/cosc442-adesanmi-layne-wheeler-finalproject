/**
 *  Copyright (C) 2002-2016   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

import org.w3c.dom.Element;


/**
 * The message sent updating a unit's current stop.
 */
public class SetCurrentStopMessage extends DOMMessage {

    public static final String TAG = "setCurrentStop";
    private static final String INDEX_TAG = "index";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the unit whose stop is to be set. */
    private final String unitId;

    /** The index of the new stop. */
    private final String index;


    /**
     * Create a new <code>SetCurrentStopMessage</code> for the
     * supplied unit.
     *
     * @param unit A <code>Unit</code> whose stop is to be setd.
     * @param index The stop index.
     */
    public SetCurrentStopMessage(Unit unit, int index) {
        super(getTagName());

        this.unitId = unit.getId();
        this.index = String.valueOf(index);
    }

    /**
     * Create a new <code>SetCurrentStopMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SetCurrentStopMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.index = getStringAttribute(element, INDEX_TAG);
    }


    /**
     * Handle a "setCurrentStop"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> the message is from.
     * @return An set containing the unit after updating its
     *     current stop, or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        ServerUnit serverUnit;
        try {
            serverUnit = serverPlayer.getOurFreeColGameObject(this.unitId,
                ServerUnit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        TradeRoute tr = serverUnit.getTradeRoute();
        if (tr == null) {
            return serverPlayer.clientError("Unit has no trade route: "
                + this.unitId)
                .build(serverPlayer);
        }

        int count;
        try {
            count = Integer.parseInt(this.index);
        } catch (NumberFormatException nfe) {
            return serverPlayer.clientError("Stop index is not an integer: " +
                this.index)
                .build(serverPlayer);
        }
        if (count < 0 || count > tr.getStops().size()) {
            return serverPlayer.clientError("Invalid stop index: "
                + this.index)
                .build(serverPlayer);
        }

        // Valid, set.
        return server.getInGameController()
            .setCurrentStop(serverPlayer, serverUnit, count)
            .build(serverPlayer);
    }

    /**
     * Convert this SetCurrentStopMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            INDEX_TAG, this.index).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "setCurrentStop".
     */
    public static String getTagName() {
        return TAG;
    }
}
