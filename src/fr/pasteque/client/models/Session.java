/*
    Pasteque Android client
    Copyright (C) Pasteque contributors, see the COPYRIGHT file

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package fr.pasteque.client.models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Session implements Serializable {

    private User user;
    private List<Ticket> runningTickets;
    private Ticket currentTicket;
    private int ticketNumber;
    private List<TicketObserver> ticketObservers;

    /** Create an empty session */
    public Session() {
        this.runningTickets = new ArrayList<Ticket>();
        this.ticketNumber = 1;
        this.ticketObservers = new ArrayList<TicketObserver>();
    }

    public void setUser(User u) {
        this.user = u;
    }

    public User getUser() {
        return this.user;
    }
   
    /** Create a new ticket and register it as current.
     * @return The created ticket
     */
    public Ticket newTicket() {
        Ticket t = new Ticket(String.valueOf(this.ticketNumber++));
        this.runningTickets.add(t);
        this.currentTicket = t;
        return t;
    }

    /** Create a new ticket for a given table.
     * To match sharedticket table the id of the ticket is set to the id of
     * the table.
     */
    public Ticket newTicket(Place p) {
        Ticket t = new Ticket(p.getId(), p.getName());
        this.runningTickets.add(t);
        this.currentTicket = t;
        this.ticketUpdated();
        return t;
    }

    public void updateTicket(Ticket t) {
        boolean	exist = false;
        for (int i = 0; i < runningTickets.size(); ++i) {
            if (runningTickets.get(i).getLabel().equals(t.getLabel())) {
                runningTickets.set(i, t);
                exist = true;
                break;
            }
        }
        if (!exist) {
            runningTickets.add(t);
        }
        this.ticketUpdated();
    }

    public void closeTicket(Ticket t) {
        this.runningTickets.remove(t);
        this.ticketUpdated();
    }

    /** Get the list of shared tickets. You should not edit this list
     * directly for observers to be notified. */
    public List<Ticket> getTickets() {
        return this.runningTickets;
    }

    public Ticket getTicket(String id) {
        for (Ticket t : runningTickets) {
            if (t.getId() != null && t.getId().equals(id))
                return t;
        }
        return null;
    }

    /** Check if there is a non empty ticket pending */
    public boolean hasRunningTickets() {
        for (Ticket t : this.runningTickets) {
            if (!t.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Check if there is at least two tickets pending */
    public boolean hasWaitingTickets() {
	boolean one = false;
	for (Ticket t : this.runningTickets) {
	    if (one) {
		return true;
	    }
	    one = true;
	}
	return false;
    }

    public boolean hasTicket() {
        return this.runningTickets.size() > 0;
    }

    public void setCurrentTicket(Ticket t) {
        this.currentTicket = t;
    }

    public Ticket getCurrentTicket() {
        return this.currentTicket;
    }

    /** Trigger observers */
    private void ticketUpdated() {
        for (TicketObserver o : this.ticketObservers) {
            o.sharedTicketsChanged(this);
        }
    }
    public void addObserver(TicketObserver o) {
        this.ticketObservers.add(o);
    }
    public void removeObserver(TicketObserver o) {
        this.ticketObservers.remove(o);
    }

    public static interface TicketObserver {
        public void sharedTicketsChanged(Session s);
    }
}
