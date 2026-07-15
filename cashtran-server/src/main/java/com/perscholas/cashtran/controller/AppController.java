package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dao.AccountDAO;
import com.perscholas.cashtran.dao.TransferDAO;
import com.perscholas.cashtran.dao.UserDao;
import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.Transfer;
import com.perscholas.cashtran.model.TransferDTO;
import com.perscholas.cashtran.model.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api")
public class AppController {

    @Autowired
    AccountDAO accountDao;

    @Autowired
    UserDao userDao;

    @Autowired
    TransferDAO transferDAO;

    // gets user balance
    @GetMapping("/balance")
    public BigDecimal getAccountBalance(Principal principal){
        String username = principal.getName();
        long userId = userDao.findIdByUsername(username);
        BigDecimal balance = accountDao.getBalance(userId);
        return balance;
    }

    // gets user account ID
    @GetMapping("/account/{id}")
    public Account getAccountByUserId(@PathVariable long id){
        return accountDao.getAnAccountByUserId(id);
    }

    // gets user ID
    @GetMapping("/user/{id}")
    public long getUserIdByAccountId(@PathVariable long id){
        return userDao.findIdByAccountID(id);
    }

    // gets list of all users
    @GetMapping("/users")
    public List<User> getAllUsers(Principal principal){
        String username = principal.getName();
        long userID = userDao.findIdByUsername(username);
        return userDao.findAll(userID);
    }

    // gets list of transfers based on user
    @GetMapping("/transfers")
    public List<Transfer> listTransfers(Principal principal){
        String username = principal.getName();
        long userID = userDao.findIdByUsername(username);
        Account account = accountDao.getAnAccountByUserId(userID);
        long accountId = account.getAccountId();
        List<Transfer> transferList = transferDAO.getAllApprovedTransfers(accountId);
        return  transferList;
    }

    // gets list of pending transfers if any based on user
    @GetMapping("/transfers/pending")
    public List<Transfer> listPendingTransfers(Principal principal){
        String username = principal.getName();
        long userID = userDao.findIdByUsername(username);
        Account account = accountDao.getAnAccountByUserId(userID);
        long accountId = account.getAccountId();
        List<Transfer> transferList = transferDAO.getAllPendingTransfers(accountId);
        return  transferList;
    }

    // gets active transfers
    @GetMapping("/transfers/{transferId}")
    public Transfer transferDetails (@PathVariable long transferId){
        return transferDAO.getTransferById(transferId);
    }

    // send money (immediate transfer)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/transfers/send")
    public Transfer sendMoney(Principal principal, @Valid @RequestBody TransferDTO transferDTO) {
        String username = principal.getName();
        long userID = userDao.findIdByUsername(username);
        Transfer transfer = transferDAO.newTransfer(userID, transferDTO.getUserId(), transferDTO.getAmount());
        return transfer;
    }

    // initializes requested transfer (for request workflow)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/transfers")
    public Transfer startTransfer (Principal principal, @Valid @RequestBody TransferDTO transferDTO) {
        String username = principal.getName();
        long userID = userDao.findIdByUsername(username);
        Transfer transfer = transferDAO.newTransfer(userID, transferDTO.getUserId(), transferDTO.getAmount());
        return transfer;
    }

    // submits a transfer request
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/requests")
    public Transfer requestTransfer(Principal principal, @Valid @RequestBody TransferDTO transferDTO){
        String username = principal.getName();
        long userId = userDao.findIdByUsername(username);
        Transfer transfer = transferDAO.newRequest(transferDTO.getUserId(), userId, transferDTO.getAmount());
        return transfer;
    }

    // submits acceptance to transfer request
    @PutMapping("/transfer/{transferId}/accept")
    public boolean acceptTransfer(Principal principal, @Valid @RequestBody TransferDTO transferDTO, @PathVariable long transferId) {
        String usernameFrom = principal.getName();
        long userFromId = userDao.findIdByUsername(usernameFrom);
        return transferDAO.acceptRequest(userFromId, transferDTO.getUserId(), transferDTO.getAmount(), transferId);
    }

    // submits rejection and cancels transfer request
    @PutMapping("/transfer/{transferId}/reject")
    public boolean rejectTransfer(Principal principal, @Valid @RequestBody TransferDTO transferDTO, @PathVariable long transferId) {
        String usernameFrom = principal.getName();
        long userFromId = userDao.findIdByUsername(usernameFrom);
        return transferDAO.rejectRequest(transferId);
    }

    // gets user account id based on username
    @GetMapping("/username/{accountId}")
    public String username (@PathVariable long accountId){
        return userDao.findUserByAccountID(accountId);
    }
}